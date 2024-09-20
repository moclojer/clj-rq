(ns com.moclojer.rq.queue
  (:refer-clojure :exclude [pop! range])
  (:require
   [clojure.string :as str]
   [com.moclojer.internal.reflection :as reflection]
   [com.moclojer.rq.adapters :as adapters]))

;; The allowlisted redis commands followed by their respective
;; documentation, param names and default encoding/decoding formats.
;; `lpush` for example encodes a given `value` through the `:edn-array`,
;; and decodes the result through the `:none` format (`identity`).

(def allowmap
  {"lpush"     ["Pushes a message into a queue"
                ["key" "string"] :edn-array :none]
   "rpush"     ["Pushes a message into a queue"
                ["key" "string"] :edn-array :none]
   "lpop"      ["Left-Pops a message from a queue"
                ["key" "count"] :none :edn-array]
   "rpop"      ["Right-Pops a message from a queue"
                ["key" "count"] :none :edn-array]
   "brpop"     ["Right-Pops a message from a queue (blocking)"
                ["timeout" "key"] :none :edn-array]
   "blpop"     ["Left-Pops a message from a queue (blocking)"
                ["timeout" "key"] :none :edn-array]
   "lindex"    ["Get the element from a queue at given index"
                ["key", "index"] :none :edn-array]
   "lrange"    ["Get the elements from a queue"
                ["key" "start" "stop"] :none :edn-array]
   "lset"      ["Sets the element from a queue at given index"
                ["key" "index" "value"] :edn :none]
   "lrem"      ["Removes matching count of given message from a queue"
                ["key" "count" "value"] :edn :none]
   "llen"      ["Gets the length of a queue"
                ["key"] :none :none]
   "linsert"   ["Inserts a message into a queue in reference to given pivot"
                ["key" "where" "pivot" "value"] :edn :none]
   "ltrim"     ["Trim a queue between the given limit values"
                ["key" "start" "stop"]
                :none :none]})

(doseq [[method parameters] (reflection/get-klazz-methods
                             redis.clients.jedis.JedisPooled
                             allowmap)]
  (let [method' (str/replace method #"[`0-9]" "")
        _base-doc (str "Wraps redis.clients.jedis.JedisPooled." method')]
    (intern
     *ns* (symbol method')
     (eval `(reflection/->wrap-method ~method ~parameters ~allowmap)))))

;; --- directional ---

(defn push!
  [client queue-name values & [options]]
  (let [{:keys [direction]
         :or {direction :l}} options
        push-fn (if (= direction :l) lpush rpush)]
    (apply push-fn [client queue-name values options])))

(defn pop!
  [client queue-name count & [options]]
  (let [{:keys [direction timeout]
         :or {direction :r}} options
        pop-fn (if (= direction :r)
                 (if timeout brpop rpop)
                 (if timeout blpop lpop))
        num (or timeout count)]
    (apply pop-fn (flatten [client
                            (if timeout
                              [num queue-name]
                              [queue-name num])
                            options]))))

(defn bpop!
  [client queue-name timeout & [options]]
  (apply pop! [client queue-name count
               (assoc options :timeout timeout)]))

(defn index
  [client queue-name index & [options]]
  (first (apply lindex [client queue-name index options])))

(defn range
  [client queue-name start stop & [options]]
  (apply lrange [client queue-name start stop options]))

(defn set!
  [client queue-name index value & [options]]
  (apply lset [client queue-name index value options]))

(defn len
  [client queue-name & [options]]
  (apply llen [client queue-name options]))

(defn rem!
  [client queue-name count value & [options]]
  (apply lrem [client queue-name count value options]))

(defn insert!
  [client queue-name where pivot value & [options]]
  (apply linsert [client queue-name
                  (adapters/->list-position where)
                  pivot value options]))

(defn trim!
  [client queue-name start stop & [options]]
  (apply ltrim [client queue-name start stop options]))
