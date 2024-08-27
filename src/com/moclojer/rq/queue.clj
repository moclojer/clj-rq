(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop!])
  (:require
   [com.moclojer.internal.reflection :as reflection]))

;; The allowlisted redis commands followed by their respective
;; documentation and default encoding/decoding formats.
;; `lpush` for example encodes a given `value` through the `:edn-array`,
;; and decodes the result through the `:none` format (`identity`).

(def allowlist
  {"lpush"     ["Pushes a message into a queue" :edn-array :none]
   "rpush"     ["Pushes a message into a queue" :edn-array :none]
   "lpop"      ["Left-Pops a message from a queue" :none :edn-array]
   "rpop"      ["Right-Pops a message from a queue" :none :edn-array]
   "brpop"     ["Right-Pops a message from a queue (blocking)"
                :none :edn-array]
   "blpop"     ["Left-Pops a message from a queue (blocking)"
                :none :edn-array]
   "lrange"    ["Get the elements from a queue" :none :edn-array]
   "lindex"    ["Get the element from a queue at given index"
                :none :none]
   "lset"      ["Sets the element from a queue at given index"
                :edn :none]
   "lrem"      ["Removes matching count of given message from a queue"
                :edn :none]
   "llen"      ["Gets the length of a queue" :none :none]
   "linsert"   ["Inserts a message into a queue in reference to
                 given pivot" :edn :none]
   "ltrim"     ["Trim a queue between the given limit values"
                :none :none]})

(doseq [[method parameters] (reflection/get-klazz-methods
                             redis.clients.jedis.JedisPooled
                             (set (keys allowlist)))]
  (eval `(reflection/->wrap-method ~method ~parameters ~allowlist)))
