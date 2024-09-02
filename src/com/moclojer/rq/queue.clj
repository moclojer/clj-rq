(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [com.moclojer.internal.reflection :as reflection]))

;; The allowlisted redis commands followed by their respective
;; arity, documentation and default encoding/decoding formats.
;; `lpush` for example encodes a given `value` through the `:edn-array`,
;; and decodes the result through the `:none` format (`identity`).

(def allowmap
  {"lpush"     [2 "Pushes a message into a queue" :edn-array :none]
   "rpush"     [2 "Pushes a message into a queue" :edn-array :none]
   "lpop"      [2 "Left-Pops a message from a queue" :none :edn-array]
   "rpop"      [2 "Right-Pops a message from a queue" :none :edn-array]
   "brpop"     [2 "Right-Pops a message from a queue (blocking)"
                :none :edn-array]
   "blpop"     [2 "Left-Pops a message from a queue (blocking)"
                :none :edn-array]
   "lrange"    [3 "Get the elements from a queue" :none :edn-array]
   "lindex"    [2 "Get the element from a queue at given index"
                :none :none]
   "lset"      [3 "Sets the element from a queue at given index"
                :edn :none]
   "lrem"      [2 "Removes matching count of given message from a queue"
                :edn :none]
   "llen"      [1 "Gets the length of a queue" :none :none]
   "linsert"   [4 "Inserts a message into a queue in reference to given pivot"
                :edn :none]
   "ltrim"     [3 "Trim a queue between the given limit values"
                :none :none]})

(doseq [[method parameters] (reflection/get-klazz-methods
                             redis.clients.jedis.JedisPooled
                             allowmap)]
  (eval `(reflection/->wrap-method ~method ~parameters ~allowmap)))
