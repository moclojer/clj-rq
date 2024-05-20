(ns com.moclojer.rq.pubsub
  (:import [redis.clients.jedis JedisPubSub]))

;; (pubsub/publish redis-client "name-subs" "value set")
(defn publish
  "Publish a message to a channel"
  [redis-client channel message]
  (.publish @redis-client channel message))

(defn listener
  [callback]
  (proxy [JedisPubSub] []
    (onMessage [channel message]
      (try
        (println "onMessage" channel message)
        (callback channel message)
        (catch Exception e
          (ex-message e) nil)))))

;; (pubsub/subscribe redis-client ["name-subs"])
(defn subscribe
  "Subscribe to channels and call the callback function when a message is received
   is possible to subscribe to multiple channels"
  [redis-client on-msg-fn channels]
  (.subscribe @redis-client (listener on-msg-fn) (into-array channels)))
