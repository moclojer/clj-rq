(ns com.moclojer.rq.queue-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.queue :as rq-queue]
   [com.moclojer.test-helpers :as helpers]))

(t/deftest queue-test
  (let [client (rq/create-client "redis://localhost:6379")
        message (helpers/gen-message)
        other-message (helpers/gen-message)]

    (let [queue-name (str (random-uuid))]
      (t/testing "raw"
        (rq-queue/lpush client queue-name [message other-message])
        (t/is (= 2 (rq-queue/llen client queue-name)))
        (t/is (= [message other-message]
                 (rq-queue/rpop client queue-name 2)))))

    (let [queue-name (str (random-uuid))]
      (t/testing "pattern"
        (rq-queue/rpush client queue-name [message] {:pattern :pending})
        (t/is
         (= [message]
            (rq-queue/rpop client queue-name 1 {:pattern :pending})))))

    (rq/close-client client)))

(comment
  (dotimes [_ 10]
    (queue-test))

  (def my-client (rq/create-client "redis://localhost:6379"))

  (dotimes [_ 10]
    (println
     (rq-queue/lpush my-client "my-queue"
                     [(helpers/gen-message)]
                     {:pattern :pending})
     (first (rq-queue/rpop my-client "my-queue" 1
                           {:pattern :pending}))))

  (rq/close-client my-client)
  ;;
  )
