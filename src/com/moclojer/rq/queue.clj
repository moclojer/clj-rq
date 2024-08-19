(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [com.moclojer.rq.utils :as utils]
   [clojure.string :as s]))

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
        return (if (= direction :l)
                 (.blpop @client tmot packed-queue-name)
                 (.brpop @client tmot packed-queue-name))]
    (when return
      (let [message (second return)]
        (log/debug "popped from queue"
                   {:client client
                    :queue-name packed-queue-name
                    :options {:direction direction :pattern pattern}
                    :message message})
        (edn/read-string message)))))

(defn lindex
  "Return a element in a specified index
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - index: specific index to access"
  [client queue-name index & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        return (.lindex @client packed-queue-name index)]

    (let [message (clojure.edn/read-string return)]
      (log/debug "message found"
                 {:client client
                  :queue-name packed-queue-name
                  :index index
                  :message message})
      message)))

(defn lset
  "Set a new message in a specified index

  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - index: specific index to access
  - message: new msg to be added"
  [client queue-name index message & options]
  (let [{:keys [pattern]
         :or {pattern :rq} :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (pr-str message)
        return (.lset @client packed-queue-name index encoded-message)]

    (log/debug "set in queue"
               {:client client
                :queue-name packed-queue-name
                :message (str encoded-message)
                :index index
                :options opts
                :return return})
    return))

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
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (pr-str msg)
        return (.lrem @client packed-queue-name cnt encoded-message)]

    (log/debug "removed from queue"
               {:client client
                :queue-name queue-name
                :msg msg
                :count cnt
                :return return})
    return))

(defn linsert
  "insert a message before the first occurance of a pivot given.

  parameters:
  - client: redis client
  - queue-name: name of the queue
  - msg: new msg to be added
  - pivot: pivot message to be added before or after
  - options:
    - pos (keywords):
      - before: insert the message before the pivot
      - after: insert the message after the pivot"
  [client queue-name pivot msg & options]
  (let [{:keys [pos pattern]
         :or {pos :before
              pattern :rq} :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (pr-str msg)
        encoded-pivot (pr-str pivot)
        encoded-pos (if (= pos :before)
                      redis.clients.jedis.args.ListPosition/BEFORE
                      redis.clients.jedis.args.ListPosition/AFTER)
        return (.linsert @client packed-queue-name encoded-pos encoded-pivot encoded-message)]
    (log/debug "inserted in queue"
               {:client client
                :queue-name queue-name
                :msg encoded-message
                :opts opts
                :return return})
    return))

(defn lrange
  "Return an entire range given min and max indexes
  
  Parameters:
  - client: Redis client
  - queue-name: Name of the queue
  - floor: floor index
  - ceil: ceiling index"
  [client queue-name floor ceil & options]
  (let [{:keys [pattern]
         :or {pattern :rq} :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        return (.lrange @client packed-queue-name floor ceil)]
    (log/debug "queue specified range"
               {:client client
                :queue-name packed-queue-name
                :opts opts
                :result return})
    (mapv clojure.edn/read-string return)))

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
         :or {pattern :rq} :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)]
    (let [return (.ltrim @client packed-queue-name start stop)]
      (log/debug "queue trimmed"
                 {:client client
                  :queue-name queue-name
                  :opts opts
                  :result return})
      return)))

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
        packed-destination-queue (utils/pack-pattern pattern destination-queue)
        return (.rpoplpush @client packed-source-queue packed-destination-queue)]
    (log/debug "rpoplpush operation"
               {:client client
                :source-queue packed-source-queue
                :destination-queue packed-destination-queue
                :result return})
    return))

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
        packed-destination-queue (utils/pack-pattern pattern destination-queue)
        result (.brpoplpush @client packed-source-queue packed-destination-queue timeout)]
    (log/debug "brpoplpush operation"
               {:client client
                :source-queue packed-source-queue
                :destination-queue packed-destination-queue
                :timeout timeout
                :result result})
    result))

(defn lmove
  "Atomically return and remove the first/last element of the source list, and push the element as the first/last element of the destination list.

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
        packed-destination-queue (utils/pack-pattern pattern destination-queue)
        from-direction (if (= wherefrom "LEFT")
                         redis.clients.jedis.args.ListDirection/LEFT
                         redis.clients.jedis.args.ListDirection/RIGHT)
        to-direction (if (= whereto "LEFT")
                       redis.clients.jedis.args.ListDirection/LEFT
                       redis.clients.jedis.args.ListDirection/RIGHT)
        result (.lmove @client packed-source-queue packed-destination-queue from-direction to-direction)]
    (log/debug "lmove operation"
               {:client client
                :source-queue packed-source-queue
                :destination-queue packed-destination-queue
                :from-direction from-direction
                :to-direction to-direction
                :result result})
    result))
