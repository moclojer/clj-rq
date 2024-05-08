(ns clj-rq.core
  (:import redis.clients.jedis JedisPool))

;; redis connection pool to be thread safe
(def ^{:private true} *redis-pool* (ref nil))

(defn redis-client
  "redis connect client"
  [url]
  (dosync
    (ref-set *redis-pool* (JedisPool. url)))
  (let [redis (.getResource @*redis-pool*)]
    (.select redis 0)
    redis))

(defn redis-client-kill
  []
  (.destroy @*redis-pool*))

(defn redis-client-disconnect
  []
  (.returnResource @*redis-pool*))
