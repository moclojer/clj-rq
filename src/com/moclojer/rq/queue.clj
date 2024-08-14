(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [com.moclojer.internal.reflection :as reflection]))

(def allowlist
  #{"lpush" "rpush" "lpop" "rpop"  "brpop"
    "blpop" "lrange" "lindex" "lset" "lrem"
    "llen" "linsert" "ltrim" "rpoplpush"
    "brpoplpush"  "lmove"})

(def commands
  (for [[method parameters] (reflection/get-klazz-methods
                             redis.clients.jedis.JedisPooled
                             allowlist)]
    (eval `(reflection/->wrap-method ~method ~parameters))))
