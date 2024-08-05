(ns com.moclojer.rq.utils-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq.utils :as utils]))

(t/deftest pattern->str-test
  (t/testing "packing"
  [(t/is "my-queue" (utils/pack-pattern :none "my-queue"))
   (t/is "rq:my-queue" (utils/pack-pattern :rq "my-queue"))
   (t/is "rq:pubsub:my-queue" (utils/pack-pattern :pubsub "my-queue"))
   (t/is "rq:pubsub:pending:my-queue" (utils/pack-pattern :pending "my-queue"))])

  (t/testing "unpacking"
  [(t/is "my-queue" (utils/unpack-pattern :none "my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :rq "rq:my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :pubsub "rq:pubsub:my-queue"))
   (t/is "my-queue" (utils/unpack-pattern :pending "rq:pubsub:pending:my-queue"))]))
