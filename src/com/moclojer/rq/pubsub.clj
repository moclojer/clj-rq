(ns com.moclojer.rq.pubsub
  (:require
   [clojure.edn :as edn])
  (:import
   [redis.clients.jedis JedisPubSub]))

;; (pubsub/publish redis-client "name-subs" "value set")
(defn publish!
  "Publish a message to a channel"
  [client channel message]
  (.publish @client channel (pr-str message)))

(defn create-listener
  [on-msg-fn]
  (proxy [JedisPubSub] []
    (onMessage [channel message]
      (try
        ;; TODO: switch for a logger
        (println "onMessage" channel message)
        (on-msg-fn channel (edn/read-string message))
        (catch Exception e
          (.printStackTrace e)
          ;; TODO: switch for a logger
          (prn :error (ex-message e)))))))

(defn subscribe!
  "Subscribe to channels and call the callback function when a message is received
   is possible to subscribe to multiple channels"
  [client on-msg-fn channels]
  (future (.subscribe @client (create-listener on-msg-fn) (into-array channels))))

(comment
  (import redis.clients.jedis.JedisPooled)

  (let [client (atom (JedisPooled. "redis://localhost:6379"))]
    (subscribe! client #(prn %1 %2) ["my-channel"])
    (Thread/sleep 1000)
    (publish! client "my-channel" {:hello true}))
  ;;
  )
