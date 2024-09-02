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

    [(t/testing "raw"
       (rq-queue/lpush client queue-name [message message2])
       (t/is (= 2 (rq-queue/llen client queue-name)))
       (t/is (= [message message2]
                (rq-queue/rpop client queue-name 2))))

     (t/testing "pattern"
       (rq-queue/rpush client queue-name [message] {:pattern :pending})
       (t/is
        (= [message]
           (rq-queue/rpop client queue-name 1 {:pattern :pending}))))]

    (rq/close-client client)))

(comment
  (t/testing "bpop! left"
    (while (not (nil? (rq-queue/bpop! client queue-name 1 {:direction :l}))))
    (rq-queue/push! client queue-name message)
    (let [popped-message (rq-queue/bpop! client queue-name 1 {:direction :l})]
      (t/is (= message popped-message))
      (t/is (= 0 (rq-queue/llen client queue-name)))))

  (t/testing "bpop! right"
    (while (not (nil? (rq-queue/bpop! client queue-name 1 {:direction :r}))))
    (rq-queue/push! client queue-name message)
    (let [popped-message (rq-queue/bpop! client queue-name 1 {:direction :r})]
      (t/is (= message popped-message))
      (t/is (= 0 (rq-queue/llen client queue-name)))))

  (t/testing "lindex"
    (rq-queue/push! client queue-name message)
    (t/is (= message (rq-queue/lindex client queue-name 0))))

  (t/testing "lset"
    (while (not (nil? (rq-queue/bpop! client queue-name 1 {:direction :l}))))
    (rq-queue/push! client queue-name message)
    (rq-queue/push! client queue-name another-message)
    (rq-queue/lset client queue-name 0 another-message)
    (t/is (= another-message (rq-queue/lindex client queue-name 0)))
    (rq-queue/lset client queue-name 1 message)
    (t/is (= message (rq-queue/lindex client queue-name 1)))
    (rq-queue/pop! client queue-name :direction :l)
    (rq-queue/pop! client queue-name :direction :l))

  (t/testing "lrem"
    (rq-queue/push! client queue-name message)
    (rq-queue/lrem client queue-name 1 message)
    (t/is (= 0 (rq-queue/llen client queue-name))))

  (t/testing "linsert"
    (rq-queue/push! client queue-name message)
    (rq-queue/linsert client queue-name message another-message :pos :before)
    (t/is (= another-message (rq-queue/lindex client queue-name 0)))
    (rq-queue/pop! client queue-name :direction :l)
    (rq-queue/pop! client queue-name :direction :l))

  (t/testing "lrange"
    (rq-queue/push! client queue-name message)
    (rq-queue/push! client queue-name another-message)
    (let [result (rq-queue/lrange client queue-name 0 1)]
      (t/is (= [message another-message] (reverse result))))
    (rq-queue/pop! client queue-name :direction :l)
    (rq-queue/pop! client queue-name :direction :l))

  (t/testing "ltrim"
    (let [base-message {:test "hello", :my/test2 "123", :foobar ["321"]}
          message (assoc base-message :uuid (java.util.UUID/randomUUID))
          another-message (assoc base-message :uuid (java.util.UUID/randomUUID))]
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name another-message)
      (t/is (= "OK" (rq-queue/ltrim client queue-name 1 -1)))
      (let [result (rq-queue/lrange client queue-name 0 -1)]
        (t/is (= [(dissoc another-message :uuid)]
                 (map #(dissoc % :uuid) result)))))
    (rq-queue/pop! client queue-name :direction :l)
    (rq-queue/pop! client queue-name :direction :l))

  (t/testing "rpoplpush"
    (rq-queue/push! client queue-name message)
    (rq-queue/rpoplpush client queue-name another-queue-name)
    (t/is (= 0 (rq-queue/llen client queue-name)))
    (t/is (= message (rq-queue/pop! client another-queue-name :direction :l))))

  (t/testing "brpoplpush"
    (rq-queue/push! client queue-name message)
    (rq-queue/brpoplpush client queue-name another-queue-name 1)
    (t/is (= 0 (rq-queue/llen client queue-name)))
    (t/is (= message (rq-queue/pop! client another-queue-name :direction :l))))

  (t/testing "lmove"
    (rq-queue/push! client queue-name message)
    (rq-queue/lmove client queue-name another-queue-name "LEFT" "RIGHT")
    (t/is (= 0 (rq-queue/llen client queue-name)))
    (t/is (= message (rq-queue/pop! client another-queue-name :direction :r)))))

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
