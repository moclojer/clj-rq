(ns com.moclojer.rq.pubsub
  (:import [redis.clients.jedis JedisPubSub]))

;; (pubsub/publish redis-client "name-subs" "value set")
(defn publish
  "Publish a message to a channel"
  [redis-client channel message]
  (.publish redis-client channel message))

(defn listener [callback]
  (try
    (proxy [JedisPubSub] []
      (onMessage [channel message]
        (callback channel message)))
    (catch Exception e
      (ex-message e) nil)))

;; (pubsub/subscribe redis-client ["name-subs"])
(defn subscribe
  "Subscribe to a channel"
  [redis-client channels]
  (.subscribe (listener redis-client) channels))
