(ns com.moclojer.rq.queue
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [com.moclojer.rq.utils :as utils]))

(defn push!
  "Push a messagem into a queue"
  [client queue-name message & options]
  (let [{:keys [direction pattern _at _in _retry _retry-delay]
         :or {direction :l
              pattern :rq}
         :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        encoded-message (into-array [(pr-str message)])
        pushed-count (if (= direction :l)
                       (.lpush @client packed-queue-name encoded-message)
                       (.rpush @client packed-queue-name encoded-message))]

    (log/debug "pushed to queue"
               {:queue-name packed-queue-name
                :message message
                :options opts
                :pushed-count pushed-count})

    pushed-count))

(defn pop!
  "Pop a message from a queue"
  [client queue-name & options]
  (let [{:keys [direction pattern]
         :or {direction :l
              pattern :rq}
         :as opts} options
        packed-queue-name (utils/pack-pattern pattern queue-name)
        message (if (= direction :l)
                  (.lpop @client packed-queue-name)
                  (.rpop @client packed-queue-name))]

    (log/debug "popped from queue"
               {:queue-name packed-queue-name
                :options opts
                :message message})

    (edn/read-string message)))

(defn llen
  "get size of a queue"
  [client queue-name & options]
  (let [{:keys [pattern]
         :or {pattern :rq}} options]
    (.llen @client (utils/pack-pattern pattern queue-name))))
