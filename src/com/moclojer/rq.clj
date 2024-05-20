(ns com.moclojer.rq
  (:require [com.moclojer.rq.pubsub :as pubsub])
  (:import [redis.clients.jedis JedisPool JedisPoolConfig JedisPooled]))

(def version "0.1.0")

;; redis connection pool to be thread safe
(def
  ^{:private true :dynamic true}
  *redis-pool* (ref nil))

(defn redis-client
  "redis connect client"
  [url]
  (when-not @*redis-pool*
    (dosync
     (ref-set *redis-pool* (JedisPooled. url))))
  *redis-pool*)

(defn redis-client-kill
  "redis kill client"
  []
  (.destroy @*redis-pool*))

(defn redis-client-disconnect
  "redis disconnect client"
  []
  (.returnResource @*redis-pool*))

(defn -main
  [& _]
  (let [redis-client-ed (JedisPooled. "redis://localhost:6379/0")
        redis-client (JedisPool. (JedisPoolConfig.) "localhost" 6379 0)]
    ;; (println "redis-client" (.getPubSub redis-client))
    (println :resource (.getResource redis-client))
    #_(pubsub/publish redis-client-ed "name-subs" "value set")
    (pubsub/subscribe (.getResource redis-client) "name-subs"))

  #_(loop [sub (pubsub/subscribe redis-client "name-subs")]))
    ;; (pubsub/subscribe redis-client "name-subs")))

(comment
  (def pooled-client (redis-client "redis://localhost:6379"))

  (pubsub/subscribe pooled-client #(prn :chan %1 :msg %2) ["hello.world" "bye.world"])
  ;;
  )
