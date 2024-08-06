(ns com.moclojer.rq.queue-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]
   [com.moclojer.rq.queue :as rq-queue]
   [com.moclojer.test-utils :as utils]))

(t/deftest queue-test
  (let [client (rq/create-client "redis://localhost:6379")
        queue-name (str (random-uuid))
        another-queue-name (str (random-uuid))
        message (utils/gen-message)
        another-message (utils/gen-message)]

    (t/testing "raw"
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name another-message)
      (t/is (= 2 (rq-queue/llen client queue-name)))
      (t/is (= message (rq-queue/pop! client queue-name {:direction :r}))))

    (t/testing "direction"
      (rq-queue/push! client queue-name message :direction :r)
      (t/is (= message (rq-queue/pop! client queue-name :direction :r))))

    (t/testing "pattern"
      (rq-queue/push! client queue-name message :pattern :pending)
      (t/is (= message (rq-queue/pop! client queue-name :pattern :pending))))

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

    (t/testing "lset"
      (while (not (nil? (rq-queue/bpop! client queue-name 1 {:direction :l}))))
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name another-message)
      (rq-queue/lset client queue-name 0 another-message)
      (t/is (= another-message (rq-queue/lindex client queue-name 0)))
      (rq-queue/lset client queue-name 1 message)
      (t/is (= message (rq-queue/lindex client queue-name 1))))

    (t/testing "lrange"
      (while (not (nil? (rq-queue/bpop! client queue-name 1 {:direction :l}))))
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name another-message)
      (t/is (= [message another-message] (rq-queue/lrange client queue-name 0 1))))

    (t/testing "lindex"
      (rq-queue/lset client queue-name 0 message)
      (t/is (= message (rq-queue/lindex client queue-name 0))))

    (t/testing "lrem"
      (rq-queue/push! client queue-name message)
      (rq-queue/lrem client queue-name 1 message)
      (t/is (= 0 (rq-queue/llen client queue-name))))

    (t/testing "linsert"
      (rq-queue/push! client queue-name message)
      (rq-queue/linsert client queue-name another-message message :pos :before)
      (t/is (= another-message (rq-queue/lindex client queue-name 0))))

    (t/testing "ltrim"
      (rq-queue/push! client queue-name message)
      (rq-queue/push! client queue-name another-message)
      (rq-queue/ltrim client queue-name 1 -1)
      (t/is (= another-message (rq-queue/lindex client queue-name 0))))

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
      (t/is (= message (rq-queue/pop! client another-queue-name :direction :r))))

    (rq/close-client client)))
