(ns com.moclojer.rq.pubsub-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.pubsub :as rq-pubsub]
   [com.moclojer.test-utils :as utils]))

(defn build-workers
  [qtt state]
  (let [channels (repeatedly qtt #(str (random-uuid)))
        messages (repeatedly qtt utils/gen-message)
        chans-msgs (zipmap channels messages)]
    {:chans-msgs chans-msgs
     :msgs messages
     :workers (map (fn [[chan msg]]
                     {:channel chan
                      :handler (fn [_]
                                 (swap! state conj msg))})
                   chans-msgs)}))

(t/deftest pubsub-test
  (let [client (rq/create-client "redis://localhost:6379")]

    (t/testing "archiving/unarchiving"
      (let [channel (str (random-uuid))
            message (utils/gen-message)]
        (rq-pubsub/publish! client channel message)
        (Thread/sleep 500)
        (t/is
         message
         (rq-pubsub/unarquive-channel! client channel identity))))

    (t/testing "multi pub/sub"
      (let [state (atom [])
            {:keys [chans-msgs messages workers]} (build-workers 1 state)]
        (rq-pubsub/subscribe! client workers)
        (Thread/sleep 500)
        (doseq [[chan msg] chans-msgs]
          (rq-pubsub/publish! client chan msg))
        (Thread/sleep 1000)
        (t/is @state messages)))

    (rq/close-client client)))
