(ns com.moclojer.rq.adapters-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [com.moclojer.rq.adapters :as adapters]))

(t/deftest pattern->str-test
  [(t/is "my-queue" (adapters/pack-pattern :none "my-queue"))
   (t/is "rq:my-queue" (adapters/pack-pattern :rq "my-queue"))
   (t/is "rq:pubsub:my-queue" (adapters/pack-pattern :pubsub "my-queue"))
   (t/is "rq:pubsub:pending:my-queue" (adapters/pack-pattern :pending "my-queue"))]

  [(t/is "my-queue" (adapters/unpack-pattern :none "my-queue"))
   (t/is "my-queue" (adapters/unpack-pattern :rq "rq:my-queue"))
   (t/is "my-queue" (adapters/unpack-pattern :pubsub "rq:pubsub:my-queue"))
   (t/is "my-queue" (adapters/unpack-pattern :pending "rq:pubsub:pending:my-queue"))])

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
  (t/testing "keyword decoders"
    [(t/is (= "hello world" (adapters/decode :none "hello world")))
     (t/is (= {:hello? true} (adapters/decode :edn "{:hello? true}")))
     (t/is (= {:hello? true} (adapters/decode :json "{\"hello?\":true}")))
     (t/is (= ["3" "true"] (adapters/decode :array (into-array ["3" "true"]))))
     (t/is (= [3 true] (adapters/decode :edn-array (into-array ["3" "true"]))))
     (t/is (= [{:hello? true}] (adapters/decode
                                :json-array
                                (into-array ["{\"hello?\":true}"]))))]))
