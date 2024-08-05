(ns com.moclojer.rq-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq]))

;; WARNING: redis needs to be runing.
(t/deftest create-client-test
  (t/testing "redis-client being created"
    (let [client (rq/create-client "redis://localhost:6379")]
      (t/is (.. @client getPool getResource))
      (rq/close-client client))))


