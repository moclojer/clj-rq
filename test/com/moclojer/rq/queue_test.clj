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
      (rq-queue/lpush client queue-name message)
      (rq-queue/lpush client queue-name (utils/gen-message))
      (t/is (= 2 (rq-queue/llen client queue-name)))
      (t/is (= message (rq-queue/rpop client queue-name 1))))

    (t/testing "direction"
      (rq-queue/rpush client queue-name message)
      (t/is (= message (rq-queue/rpop client queue-name))))

    (t/testing "pattern"
      (rq-queue/lpush client queue-name message :pattern :pending)
      (t/is (= message (rq-queue/rpop client queue-name :pattern :pending))))

    (rq/close-client client)))

(comment
  (def my-client (rq/create-client "redis://localhost:6379"))

  (.lpush @my-client)

  (rq/close-client my-client)
  ;;
  )
