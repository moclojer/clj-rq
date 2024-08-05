(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [com.moclojer.rq.utils :as utils]
   [clojure.core.async :as async]))

(defn push!
  "Push a message into a queue.
  
  Options:

  - direction: Direction to push the message (:l or :r)
  - pattern: Pattern for the queue name"
  [client queue-name message & options]
  (let [{:keys [direction pattern _at _in _retry _retry-delay]
         :or {direction :l
              pattern :rq}
         :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (into-array [(pr-str message)])
        pushed-count (if (= direction :l)
                       (.lpush @client packed-queue-name encoded-message)
                       (.rpush @client packed-queue-name encoded-message))]

    (log/debug "pushed to queue"
               {:client client
                :queue-name packed-queue-name
                :message message
                :options opts
                :pushed-count pushed-count})

    pushed-count))

(defn pop!
  "Pop a message from a queue.

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - options: 
    - direction: Direction to pop the message (:l or :r)
    - pattern: Pattern for the queue name"
  [client queue-name & options]
  (let [{:keys [direction pattern]
         :or {direction :l
              pattern :rq}
         :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        message (if (= direction :l)
                  (.lpop @client packed-queue-name)
                  (.rpop @client packed-queue-name))]

    (when message
      (log/debug "popped from queue"
                 {:client client
                  :queue-name packed-queue-name
                  :options opts
                  :message message})

      (edn/read-string message))))

(defn llen
  "Get the size of a queue.

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - options: Optional parameters, including:
    - pattern: Pattern for the queue name"
  [client queue-name & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options]
    (.llen @client (utils/pack-pattern pattern queue-name))))

(defn bpop!
  "Block pop a message
  
  Parameters:
  - same as pop!
    - client: Redis client
    - queue-name: Name of the queue
    - options: Optional parameters, including:
      - pattern: Pattern for the queue name
  - timeout: Timeout for retrying to pop!"
  ([client queue-name timeout & options]
  (or (apply (partial pop! client queue-name) options)
      (if (>= timeout 1000)
        (do
          (Thread/sleep 1000)
          (bpop! client queue-name (- timeout 1000) options))
        nil))))

(defn lrange 
  "Return an entire range given min and max indexes
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - min: floor index
  - max: ceiling index"
  [client queue-name min max & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue (utils/pack-pattern pattern queue-name)]
    (.range @client packed-queue min max)))


(defn lindex
  "Return a element in a specified index
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - index: specific index to access"
  [client queue-name index & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue (utils/pack-pattern pattern queue-name)]
    (.lindex @client packed-queue index)))

(defn lset 
  "Set a new message in the specified index
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - index: specific index to access
  - msg: new msg to be added"
  [client queue-name index msg & options]
  ;TODO remove (let [old-msg (lindex client queue-name index)](println old-msg))
   (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue (utils/pack-pattern pattern queue-name)]
    (.lset @client packed-queue index msg))))

(defn lrem 
  "removes a specified occurance (defined by count) of the message given (if any)
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - msg: new msg to be added
  - cnt: count
    count > 0: Remove elements equal to element moving from head to tail.
    count < 0: Remove elements equal to element moving from tail to head.
    count = 0: Remove all elements equal to element."
  [client queue-name cnt msg & options]
   (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue (utils/pack-pattern pattern queue-name)]
    (.lrem @client packed-queue cnt msg))))

(comment 
  (defn linsert [])

  (defn ltrim [])

  (defn rpoplpush [])

  (defn brpoplpsuh [])

  (defn lmove []))
