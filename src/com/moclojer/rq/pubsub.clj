(ns com.moclojer.rq.pubsub
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log])
  (:import
   [redis.clients.jedis JedisPubSub]))

(defn publish!
  "Publish a message to a channel"
  [client channel message]
  (let [consumer-count (.publish @client channel (pr-str message))]

    (log/debug "published to channel"
               {:channel channel
                :message message
                :consumer-count consumer-count})

    consumer-count))

(defn create-listener
  "Create a listener for the pubsub. It will be entry point for any published
  data, being responsible for routing the right consumer. However, that's on
  the enduser."
  [on-msg-fn]
  (proxy [JedisPubSub] []
    (onMessage [channel message]
      (try
        (log/info "consumed from channel"
                  {:channel channel
                   :message message})
        (on-msg-fn channel (edn/read-string message))
        (catch Exception e
          (.printStackTrace e)
          (log/error "failed to consume from channel"
                     {:channel channel
                      :message message
                      :exception e}))))))

(defn subscribe!
  "Subscribe to channels and call the callback function when a message is received
   NOTE: It is possible to subscribe to multiple channels"
  [client on-msg-fn channels]
  (future (.subscribe @client (create-listener on-msg-fn) (into-array channels))))
