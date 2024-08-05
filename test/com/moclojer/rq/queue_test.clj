(ns com.moclojer.rq.queue-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.queue :as rq-queue]
   [com.moclojer.test-utils :as utils]))

(t/deftest queue-test
  (let [client (rq/create-client "redis://localhost:6379")
        queue-name (str (random-uuid))
        message (utils/gen-message)]

    (t/testing "raw"
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name (utils/gen-message))
      (t/is (= 2 (rq-queue/llen client queue-name)))
      (t/is (= message (rq-queue/pop! client queue-name :direction :r))))

    (t/testing "direction"
      (rq-queue/push! client queue-name message :direction :r)
      (t/is (= message (rq-queue/pop! client queue-name :direction :r))))

    (t/testing "pattern"
      (rq-queue/push! client queue-name message :pattern :pending)
      (t/is (= message (rq-queue/pop! client queue-name :pattern :pending))))

    ;; TODO
    (t/testing "block" 
      (rq-queue/bpop! client queue-name 10000 message)
      (t/is (= message (rq-queue/pop! client queue-name message)))
      )

    (rq/close-client client)))
