(ns com.moclojer.rq.utils-test
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [com.moclojer.rq.utils :as utils]))

(t/deftest pattern->str-test
  [(t/is "my-queue" (utils/pack-pattern :none "my-queue"))
   (t/is "rq:my-queue" (utils/pack-pattern :rq "my-queue"))
   (t/is "rq:pubsub:my-queue" (utils/pack-pattern :pubsub "my-queue"))
   (t/is "rq:pubsub:pending:my-queue" (utils/pack-pattern :pending "my-queue"))]

  [(t/is "my-queue" (utils/unpack-pattern :none "my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :rq "rq:my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :pubsub "rq:pubsub:my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :pending "rq:pubsub:pending:my-queue"))])

(t/deftest encode-test
  (t/testing "keyword encoders"
    [(t/is (= "hello world" (utils/encode :none "hello world")))
     (t/is (= "{:hello? true}" (utils/encode :edn {:hello? true})))
     (t/is (= "{\"hello?\":true}" (utils/encode :json {:hello? true})))
     (t/is (= ["3" "true"] (vec (utils/encode :array [3 true]))))
     (t/is (= ["{\"hello?\":true}"] (vec (utils/encode
                                          :json-array
                                          [{:hello? true}]))))])
  (t/testing "function encoder"
    (t/is (= "HELLO WORLD" (utils/encode str/upper-case "hello world")))))

(t/deftest decode-test
  (t/testing "keyword decoders"
    [(t/is (= "hello world" (utils/decode :none "hello world")))
     (t/is (= {:hello? true} (utils/decode :edn "{:hello? true}")))
     (t/is (= {:hello? true} (utils/decode :json "{\"hello?\":true}")))
     (t/is (= ["3" "true"] (utils/decode :array (into-array ["3" "true"]))))
     (t/is (= [3 true] (utils/decode :edn-array (into-array ["3" "true"]))))
     (t/is (= [{:hello? true}] (utils/decode
                                :json-array
                                (into-array ["{\"hello?\":true}"]))))]))
