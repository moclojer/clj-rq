(ns com.moclojer.rq
  (:require [com.moclojer.rq.pubsub :as pubsub])
  (:import [redis.clients.jedis JedisPooled]))

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
  (redis-client "redis://localhost:6379")
  (pubsub/publish *redis-pool* "hello.world" "value set")
  (pubsub/subscribe *redis-pool* #(prn :chan %1 :msg %2) ["hello.world"]))
