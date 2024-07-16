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
            message (utils/gen-message)
            state (atom nil)]
        (rq-pubsub/publish! client channel message)
        (Thread/sleep 500)
        (rq-pubsub/unarquive-channel! client channel (fn [msg]
                                                       (reset! state msg)))
        (t/is (= message @state))))

    (t/testing "unarchiving after subscribing"
      (let [channel (str (random-uuid))
            message (utils/gen-message)
            state (atom nil)]
        (rq-pubsub/publish! client channel message)
        (rq-pubsub/publish! client channel message)
        (rq-pubsub/subscribe! client [{:channel channel
                                       :handler (fn [msg]
                                                  (swap! state conj msg))}])
        (Thread/sleep 1000)
        (t/is (= (repeatedly 2 (constantly message)) @state))))

    (t/testing "multi pub/sub"
      (let [state (atom [])
            {:keys [chans-msgs msgs workers]} (build-workers 5 state)]
        (rq-pubsub/subscribe! client workers)
        (Thread/sleep 500)
        (doseq [[chan msg] chans-msgs]
          (rq-pubsub/publish! client chan msg))
        (Thread/sleep 1000)
        (t/is (= msgs @state))))

    (rq/close-client client)))
