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

(comment
  (let [client (create-client "redis://localhost:6379")]
    (queue/push! client "my-queue" {:user/name "john"
                                    :user/surname "doe"
                                    :user/age 24})
    (let [popped (queue/pop! client "my-queue")]
      (close-client client)
      popped))
  ;; => #:user{:name "john", :surname "doe", :age 24}

  (let [client (create-client "redis://localhost:6379")]
    (pubsub/subscribe! client #(prn :channel %1 :received %2)
                       ["my-channel" "my-other-channel"])
    (Thread/sleep 1000)
    (dotimes [_ 10]
      (pubsub/publish! client "my-channel"
                       {:topic/id :created-user
                        :user/id 123
                        :user/name "john doe"})
      (pubsub/publish! client "my-other-channel"
                       {:hello :bye
                        :try 2
                        :user-count 5}))
    (close-client client))
  ;;
  )
