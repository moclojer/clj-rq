(ns com.moclojer.rq.pubsub-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.pubsub :as rq-pubsub]
   [com.moclojer.test-utils :as utils]))

(t/deftest pubsub-test
  (let [client (rq/create-client "redis://localhost:6379")
        subs (atom [])
        channels (into [] (repeatedly 20 #(str (random-uuid))))
        messages (into [] (repeatedly 20 utils/gen-message))
        chan-msg (zipmap channels messages)]
    (rq-pubsub/subscribe! client
                          #(swap! subs conj {:channel %1 :message %2})
                          channels)
    (doseq [[channel message] chan-msg]
      (rq-pubsub/publish! client channel message))

    (future
      ;; more than enough time for 20 messages to be consumed
      (Thread/sleep 3000)
      (t/is (= (map (fn [[chan msg]]
                      {:channel chan
                       :message msg})
                    chan-msg)
               @subs)))))
