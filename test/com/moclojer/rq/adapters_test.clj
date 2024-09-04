(ns com.moclojer.rq.adapters-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [com.moclojer.rq.adapters :as adapters]))

(t/deftest pattern->str-test
  (t/are [expected pattern queue-name] (= expected
                                          (adapters/pack-pattern
                                           pattern queue-name))
    "my-queue"                   :none    "my-queue"
    "rq:my-queue"                :rq      "my-queue"
    "rq:pubsub:my-queue"         :pubsub  "my-queue"
    "rq:pubsub:pending:my-queue" :pending "my-queue")

  (t/are [expected pattern queue-name] (= expected
                                          (adapters/unpack-pattern
                                           pattern queue-name))
    "my-queue" :none    "my-queue"
    "my-queue" :rq      "rq:my-queue"
    "my-queue" :pubsub  "rq:pubsub:my-queue"
    "my-queue" :pending "rq:pubsub:pending:my-queue"))

(t/deftest encode-test
  (t/testing "keyword encoders"
    [(t/is (= "hello world" (adapters/encode :none "hello world")))
     (t/is (= "{:hello? true}" (adapters/encode :edn {:hello? true})))
     (t/is (= "{\"hello?\":true}" (adapters/encode :json {:hello? true})))
     (t/is (= ["3" "true"] (vec (adapters/encode :array [3 true]))))
     (t/is (= ["{\"hello?\":true}"] (vec (adapters/encode
                                          :json-array
                                          [{:hello? true}]))))])
  (t/testing "function encoder"
    (t/is (= "HELLO WORLD" (adapters/encode str/upper-case "hello world")))))

(t/deftest decode-test
  (t/are [expected decoding value] (= expected
                                      (adapters/decode decoding value))
    "hello world"    :none "hello world"
    {:hello? true}   :edn "{:hello? true}"
    {:hello? true}   :json "{\"hello?\":true}"
    ["3" "true"]     :array (into-array ["3" "true"])
    [3 true]         :edn-array (into-array ["3" "true"])
    [{:hello? true}] :json-array (into-array ["{\"hello?\":true}"])))
