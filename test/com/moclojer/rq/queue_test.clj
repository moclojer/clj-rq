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
      (rq-queue/lpush client queue-name [message])
      (rq-queue/lpush client queue-name [(utils/gen-message)])
      (t/is (= 2 (rq-queue/llen client queue-name)))
      (t/is (= message (first (rq-queue/rpop client queue-name 1)))))

    (t/testing "pattern"
      (rq-queue/lpush client queue-name [message] :pattern :pending)
      (t/is
       (= message
          (first (rq-queue/rpop client queue-name 1 :pattern :pending)))))

    (rq/close-client client)))

(comment
  (def my-client (rq/create-client "redis://localhost:6379"))

  (rq-queue/lpush my-client "my-queue"
                  [{:hello true
                    :names ["josue" "teodoro"]}]
                  {:pattern :pending})

  (rq-queue/rpop my-client "my-queue" 1 {:pattern :pending})

  (rq/close-client my-client)
  ;;
  )
