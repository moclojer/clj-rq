(ns com.moclojer.rq
  (:require
   [com.moclojer.rq.pubsub :as pubsub]
   [com.moclojer.rq.queue :as queue])
  (:import
   [redis.clients.jedis JedisPooled]))

(def version "0.1.1")

;; redis connection pool to be thread safe
(def
  ^{:private true :dynamic true}
  *redis-pool* (ref nil))

(defn create-client
  "redis connect client"
  ([url]
   (create-client url false))
  ([url ref?]
   (let [pool (JedisPooled. url)]
     (if (and ref? (not @*redis-pool*))
       (dosync
        (ref-set *redis-pool* pool)
        *redis-pool*)
       (atom pool)))))

(defn close-client
  "redis close client"
  ([] (close-client *redis-pool*))
  ([client] (.close @client)))
