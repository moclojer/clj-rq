(ns com.moclojer.rq.pubsub
  (:require
   [clojure.edn :as edn]
   [clojure.tools.logging :as log]
   [com.moclojer.rq.adapters :as adapters]
   [com.moclojer.rq.queue :as queue])
  (:import
   [redis.clients.jedis JedisPubSub]
   [redis.clients.jedis.exceptions JedisConnectionException]))

(defn publish!
  "Publish a message to a channel. When `consumer-min` isn't met,
  archives the message. Returns whether or not `consumer-min` was met."
  [client channel message & options]
  (let [{:keys [consumer-min]
         :or {consumer-min 1}
         :as opts} options
        consumer-count (.publish
                        @client
                        (adapters/pack-pattern :pubsub channel)
                        (pr-str message))
        consumer-met? (>= consumer-count consumer-min)
        debug-args {:channel channel
                    :message message
                    :options opts
                    :consumer-count consumer-count
                    :consumer-met? consumer-met?}]
    (if consumer-met?
      (log/debug "published message to channel" debug-args)
      (do
        (log/warn "published message, but didn't meet min consumers. archiving..."
                  debug-args)
        (queue/push! client channel [message] {:pattern :pending})))

    consumer-met?))

(defn group-handlers-by-channel
  [workers]
  (reduce
   (fn [acc {:keys [channel handler]}]
     (assoc acc channel handler))
   {} workers))

(defn create-listener
  "Create a listener for the pubsub. It will be entry point for any
  published data, being responsible for routing the right consumer.
  However, that's on the enduser."
  [workers]
  (let [handlers-by-channel (group-handlers-by-channel workers)]
    (proxy [JedisPubSub] []
      (onMessage [channel message]
        (log/info "consuming channel message"
                  {:channel channel
                   :message message})
        (if-let [handler-fn (get handlers-by-channel channel)]
          (handler-fn (if (string? message)
                        (edn/read-string message)
                        message))
          (log/error "no worker handler found for channel"
                     {:channel channel
                      :channels (keys handlers-by-channel)
                      :message message}))))))

(defn unarquive-channel!
  "Unarquives every pending message from given `channel`, calling
  `on-msg-fn` on each of them."
  [client channel on-msg-fn]
  (loop [message-count 0]
    (if-let [?message (first
                       (queue/pop! client channel 1
                                   {:pattern :pending}))]
      (do
        (try
          (on-msg-fn ?message)
          (catch Exception e
            (.printStackTrace e)
            (log/error "failed to unarchive channel message"
                       {:channel channel
                        :message ?message
                        :on-msg-fn on-msg-fn
                        :exception e
                        :ex-message (.getMessage e)})))
        (recur (inc message-count)))
      (log/info "unarchived channel"
                {:channel channel
                 :message-count message-count}))))

(defn pack-workers-channels
  [workers]
  (map
   #(update % :channel (partial adapters/pack-pattern :pubsub))
   workers))

(defn subscribe!
  "Subscribe given `workers` to their respective channels.
  
  The list of `workers` should look something like this:

  `[{:channel \"my-channel\"
     :handler (fn [msg] (do-something-with-my-msg))}
    {:channel \"my-other-channel\"
     :handler (fn [msg] (do-something-else msg))}]`

  Options:

  - reconnect-sleep: Time to sleep before reconnecting, right after an
                     abrupt or unexpected disconnection.
  - blocking?: Whether or not to block the current thread."
  [client workers & options]
  (let [packed-workers (pack-workers-channels workers)
        packed-channels (vec (map :channel packed-workers))
        listener (create-listener packed-workers)
        {:keys [reconnect-sleep blocking?]
         :or {reconnect-sleep 2500
              blocking? false}
         :as opts} options]

    (doseq [channel (map :channel workers)]
      (unarquive-channel!
       client channel
       #(.onMessage
         listener
         (adapters/pack-pattern :pubsub channel)
         %)))

    (let [sub-fn #(try
                    (.subscribe @client listener
                                (into-array packed-channels))

                    (log/debug "subscribed workers to channels"
                               {:channels packed-channels
                                :options opts})

                    (catch JedisConnectionException e

                      (log/warn "subscriber connection got killed. trying to reconnect..."
                                {:channels packed-channels
                                 :exception e
                                 :ex-message (.getMessage e)})

                      (Thread/sleep reconnect-sleep)
                      (apply subscribe! [client workers options])))]
      (if blocking?
        (sub-fn)
        (future (sub-fn))))

    listener))
