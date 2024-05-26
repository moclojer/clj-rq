(ns com.moclojer.rq
  (:require [com.moclojer.rq.queue :as queue])
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

(comment
  (redis-client "redis://localhost:6379")
  (queue/producer *redis-pool* "my-queue" {:now (java.time.LocalDateTime/now)
                                           :foo "bar"})
  (println :size (queue/consumer-size *redis-pool* "my-queue"))
  (queue/consumer *redis-pool* "my-queue" #(prn :msg %1))

  #_(pubsub/publish *redis-pool* "hello.world" "value set")
  #_(pubsub/subscribe *redis-pool* #(prn :chan %1 :msg %2) ["hello.world"]))
