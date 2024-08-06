(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [com.moclojer.rq.utils :as utils]))

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
         :or {pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)]
    (.llen @client packed-queue-name)))  

(defn bpop!
  "Pop a message from a queue. Blocking if necessary.

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - tmot: Blocking timeout
  - options:
    - direction: Direction to pop the message (:l or :r)
    - pattern: Pattern for the queue name"
  [client queue-name tmot & {:keys [direction pattern]
                             :or {direction :l pattern :rq}}]
  (let [packed-queue-name (utils/pack-pattern pattern queue-name)
        result (if (= direction :l)
                 (.blpop @client tmot packed-queue-name)
                 (.brpop @client tmot packed-queue-name))]
    (when result
      (let [message (second result)]
        (log/debug "popped from queue"
                   {:client client
                    :queue-name packed-queue-name
                    :options {:direction direction :pattern pattern}
                    :message message})
        (edn/read-string message)))))

(defn lrange 
  "Return an entire range given min and max indexes
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - floor: floor index
  - ceil: ceiling index"
  [client queue-name floor ceil & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue (utils/pack-pattern pattern queue-name)]
    (.lrange @client packed-queue floor ceil)))

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
  [client queue-name index msg & {:keys [pattern]
                                  :or {pattern :rq}}]
  (let [packed-queue-name (utils/pack-pattern pattern queue-name)]
    (.lset @client packed-queue-name index msg)))


(defn lrem 
  "Removes a specified occurance of the message given (if any)
  
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
        packed-queue-name (utils/pack-pattern pattern queue-name)]
    (.lrem @client packed-queue-name cnt msg)))


(defn linsert 
  "Insert a message before the first occurance of a pivot given.

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - msg: new msg to be added
  - pivot: pivot message to be added before or after
  - options:
    - pos (keywords):
      - before: insert the message before the pivot
      - after: insert the message after the pivot"
  [client queue-name msg pivot & options]
  (let [{:keys [pos pattern]
         :or {pos :before 
              pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)]
    (.linsert @client packed-queue-name pos pivot msg)))


(defn ltrim
  "Trim a list to the specified range.

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - start: start index
  - stop: stop index
  - options:
    - pattern: pattern to pack the queue name"
  [client queue-name start stop & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)]
    (.ltrim @client packed-queue-name start stop)))

(defn rpoplpush
  "Remove the last element in a list and append it to another list.

  Parameters:
  - client: Redis client
  - source-queue: Name of the source queue
  - destination-queue: Name of the destination queue
  - options:
    - pattern: pattern to pack the queue names"
  [client source-queue destination-queue & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-source-queue (utils/pack-pattern pattern source-queue)
        packed-destination-queue (utils/pack-pattern pattern destination-queue)]
    (.rpoplpush @client packed-source-queue packed-destination-queue)))

(defn brpoplpush
  "Remove the last element in a list and append it to another list, blocking if necessary.

  Parameters:
  - client: Redis client
  - source-queue: Name of the source queue
  - destination-queue: Name of the destination queue
  - timeout: timeout in seconds
  - options:
    - pattern: pattern to pack the queue names"
  [client source-queue destination-queue timeout & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-source-queue (utils/pack-pattern pattern source-queue)
        packed-destination-queue (utils/pack-pattern pattern destination-queue)]
    (.brpoplpush @client packed-source-queue packed-destination-queue timeout)))

(defn lmove
  "Atomically return and remove the first/last element (head/tail depending on the wherefrom argument) of the source list, and push the element as the first/last element (head/tail depending on the whereto argument) of the destination list.

  Parameters:
  - client: Redis client
  - source-queue: Name of the source queue
  - destination-queue: Name of the destination queue
  - wherefrom: 'LEFT' or 'RIGHT'
  - whereto: 'LEFT' or 'RIGHT'
  - options:
    - pattern: pattern to pack the queue names"
  [client source-queue destination-queue wherefrom whereto & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-source-queue (utils/pack-pattern pattern source-queue)
        packed-destination-queue (utils/pack-pattern pattern destination-queue)]
    (.lmove @client packed-source-queue packed-destination-queue wherefrom whereto)))
