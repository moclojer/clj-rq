(ns com.moclojer.rq.queue-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.queue :as rq-queue]
   [com.moclojer.test-helpers :as helpers]))

(t/deftest queue-test
  (let [client (rq/create-client "redis://localhost:6379")
        queue-name (str (random-uuid))
        message (helpers/gen-message)
        message2 (helpers/gen-message)]

    [(t/testing "simple"
       (rq-queue/push! client queue-name [message message2])
       (t/is (= 2 (rq-queue/len client queue-name)))
       (t/is (= [message message2]
                (rq-queue/pop! client queue-name 2))))

     (t/testing "direction"
       ;; pushing from the right, then reverse popping from the left
       (rq-queue/push! client queue-name [message message2]
                       {:direction :r})
       (t/is (= [message message2]
                (rq-queue/pop! client queue-name 2
                               {:direction :l}))))

     (t/testing "pattern"
       (rq-queue/push! client queue-name [message]
                       {:pattern :pending})
       (t/is (= [message]
                (rq-queue/pop! client queue-name 1
                               {:pattern :pending}))))

     (t/testing "blocking"
       (rq-queue/push! client queue-name [message])
       (t/is (= message
                (second (rq-queue/bpop! client queue-name 1)))))

     (t/testing "index"
       (rq-queue/push! client queue-name [message])
       (t/is (= message (rq-queue/index client queue-name 0)))
       (rq-queue/pop! client queue-name 1))

     (t/testing "range"
       (rq-queue/push! client queue-name [message message2])
       (t/is (= [message2 message]
                (rq-queue/range client queue-name 0 -1)))
       (rq-queue/pop! client queue-name 2))

     (t/testing "set!"
       (rq-queue/push! client queue-name [message message2])
       (rq-queue/set! client queue-name 0 message2)
       (rq-queue/set! client queue-name 1 message)
       (t/is (= [message message2] (rq-queue/pop! client queue-name 2))))

     (t/testing "rem!"
       (rq-queue/push! client queue-name [message message message])
       (rq-queue/rem! client queue-name 3 message)
       (t/is (= 0 (rq-queue/len client queue-name))))

     (t/testing "insert!"
       (rq-queue/push! client queue-name [message])
       (rq-queue/insert! client queue-name :before message message2)
       (t/is (= [message message2] (rq-queue/pop! client queue-name 2))))

     (t/testing "trim!"
       (let [base-message {:test "hello", :my/test2 "123", :foobar ["321"]}
             message (assoc base-message :uuid (random-uuid))
             another-message (assoc base-message :uuid (random-uuid))]
         (rq-queue/push! client queue-name [another-message message])
         [(t/is (= "OK" (rq-queue/trim! client queue-name 1 -1)))
          (t/is (= [(dissoc another-message :uuid)]
                   (map #(dissoc % :uuid)
                        (rq-queue/range client queue-name 0 -1))))])
       (rq-queue/pop! client queue-name 2))]

    (rq/close-client client)))

(comment
  (def my-client (rq/create-client "redis://localhost:6379"))

  (rq-queue/push! my-client "my-queue2" [{:hello true}])

  (rq-queue/insert! my-client "my-queue2" :before {:hello true} {:bye false})

  (rq-queue/range my-client "my-queue2" 0 -1)

  (rq-queue/len my-client "my-queue2")

  (rq-queue/pop! my-client "my-queue2" 2)

  (rq/close-client my-client)

  (rq/close-client my-client)
  ;;
  )
