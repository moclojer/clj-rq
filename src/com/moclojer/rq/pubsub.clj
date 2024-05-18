(ns com.moclojer.rq.pubsub
  (:import [redis.clients.jedis JedisPubSub]))

;; (pubsub/publish redis-client "name-subs" "value set")
(defn publish
  "Publish a message to a channel"
  [redis-client channel message]
  (.publish redis-client channel message))

(defn listener
  [callback]
  (try
    (proxy [JedisPubSub] []
      (onMessage [channel message]
        (println "onMessage" channel message)
        (callback channel message)))
    (catch Exception e
      (ex-message e) nil)))

;; (defn make-pubsub
;;   [redis-client]
;;   (doto (JedisPubSub.)
;;     (.client redis-client)))

;; (pubsub/subscribe redis-client ["name-subs"])
(defn subscribe
  "Subscribe to a channel"
  [redis-client channels]
  ;; chamar o metodo `.subscribe` do jedis nessa função, passando os parametros necessarios
  ;; (.subscribe redis-client (make-pubsub) channels)

  (.subscribe redis-client (listener (fn [channel message] (println "channel" channel "message" message))) channels))
