(ns com.moclojer.rq.queue
  (:require
   [clojure.core.async :as async]
   [clojure.edn :as edn]
   [com.moclojer.rq.utils :as utils]))

(defn push!
  [client queue-name message & options]
  ;; NOTE: maybe use clojure.tools.logging? this way the enduser chooses which
  ;; logging mechanism to use by themselves.
  (let [{:keys [direction pattern _at _in _retry _retry-delay]
         :or {direction :l
              pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (into-array [(pr-str message)])]
    (if (= direction :l)
      (.lpush @client packed-queue-name encoded-message)
      (.rpush @client packed-queue-name encoded-message))))

(comment
  (import redis.clients.jedis.JedisPooled)

  (let [client (atom (JedisPooled. "redis://localhost:6379"))]
    (push! client "my-queue" {:hello true} :direction :l)
    (.close @client))
  ;;
  )

(defn pop!
  [client queue-name & options]
  (let [{:keys [direction pattern]
         :or {direction :l
              pattern :rq}} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        message (if (= direction :l)
                  (.lpop @client packed-queue-name)
                  (.rpop @client packed-queue-name))]
    (edn/read-string message)))

(comment
  (import redis.clients.jedis.JedisPooled)

  (let [client (atom (JedisPooled. "redis://localhost:6379"))]
    (push! client "my-queue" {:hello true} :direction :r)
    (let [popped-val (pop! client "my-queue" :direction :r)]
      (.close @client)
      popped-val))
  ;; => {:hello true}
  )

(defn llen
  "get size of a queue"
  [client queue-name & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options]
    (.llen @client (utils/pack-pattern pattern queue-name))))

(comment
  (import redis.clients.jedis.JedisPooled)

  (let [client (atom (JedisPooled. "redis://localhost:6379"))]
    (push! client "my-queue" {:hello 1} :direction :r)
    (push! client "my-queue" {:hello 2} :direction :r)
    (push! client "my-queue" {:hello 3} :direction :r)
    (let [queue-length (llen client "my-queue")]
      (pop! client "my-queue" :direction :r)
      (pop! client "my-queue" :direction :r)
      (pop! client "my-queue" :direction :r)
      (.close @client)
      queue-length))
  ;; => 3
  )

(defn wait-and-consume!
  "returns the channel, which can be closed with async/close!"
  [client queue-name consume-fn & options]
  (let [{:keys [direction pattern sleep-time buffer-size]
         :or {direction  :l
              pattern    :rq
              sleep-time 200
              buffer-size 1024}} options
        chan (async/chan (async/sliding-buffer buffer-size))]

    ;; wait and retrieve messages
    (async/go-loop []
      (when (.isConnected @client)
        (if-let [?message (pop! client queue-name
                                :direction direction
                                :pattern pattern)]
          (do
            (Thread/sleep sleep-time)
            (when (async/>! chan ?message)
              (recur)))
          (recur))))

    ;; now consume them
    (async/go-loop []
      (when-let [?message (async/<! chan)]
        (consume-fn ?message)
        (recur)))

    chan))

(comment
  (import redis.clients.jedis.JedisPooled)

  (let [client (atom (JedisPooled. "redis://localhost:6379"))
        chan (wait-and-consume! client "my-queue" #(prn :new-message %))]
    (Thread/sleep 1000)
    (push! client "my-queue" {:hello 1})
    (Thread/sleep 250)
    (push! client "my-queue" {:hello 2})
    (Thread/sleep 2000)
    (push! client "my-queue" {:hello 3})
    (.close! chan)
    (.close @client))
  ;;
  )
