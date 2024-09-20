(ns com.moclojer.rq
  (:import
   [redis.clients.jedis JedisPooled]))

(def version "0.2.1")

;; redis connection pool to be thread safe
(def
  ^{:private true :dynamic true}
  *redis-pool* (ref nil))

(defn create-client
  "Connect to redis client. If `ref?` is true, will save the created instance
  in the global var `*redis-pool*. Just returns the created instance otherwise."
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
  "Disconnect and close redis client.
   If no specific client is passed, the global client stored is closed;"
  ([] (close-client *redis-pool*))
  ([client] (.close @client)))


