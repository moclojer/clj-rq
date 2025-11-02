(ns com.moclojer.rq-test
  (:require
   [clojure.test :as t]
   [com.moclojer.rq :as rq])
  (:import
   [org.apache.commons.pool2.impl GenericObjectPoolConfig]))

;; WARNING: redis needs to be running.
(t/deftest create-client-test
  (t/testing "redis-client being created"
    (let [client (rq/create-client "redis://localhost:6379")]
      (t/is (.. @client getPool getResource))
      (rq/close-client client))))

(t/deftest pool-config-resolution-test
  (t/testing "default settings are applied"
    (let [^GenericObjectPoolConfig cfg (#'rq/resolve-pool-config nil)]
      (t/is (= 128 (.getMaxTotal cfg)))
      (t/is (= 128 (.getMaxIdle cfg)))
      (t/is (= 16 (.getMinIdle cfg)))
      (t/is (= 1000 (.getMaxWaitMillis cfg)))
      (t/is (.getTestOnBorrow cfg))
      (t/is (.getTestOnReturn cfg))
      (t/is (.getTestOnCreate cfg))
      (t/is (.getTestWhileIdle cfg))
      (t/is (= 60000 (.getMinEvictableIdleTimeMillis cfg)))
      (t/is (= 60000 (.getSoftMinEvictableIdleTimeMillis cfg)))
      (t/is (= 30000 (.getTimeBetweenEvictionRunsMillis cfg)))
      (t/is (= 3 (.getNumTestsPerEvictionRun cfg)))
      (t/is (.getBlockWhenExhausted cfg))))

  (t/testing "custom overrides keep defaults"
    (let [^GenericObjectPoolConfig cfg (#'rq/resolve-pool-config {:max-total 64
                                                                  :max-wait-ms 2500})]
      (t/is (= 64 (.getMaxTotal cfg)))
      (t/is (= 2500 (.getMaxWaitMillis cfg)))
      (t/is (= 16 (.getMinIdle cfg)))))

  (t/testing "custom overrides without defaults"
    (let [^GenericObjectPoolConfig cfg (#'rq/resolve-pool-config {:inherit-defaults? false
                                                                  :max-total 10})]
      (t/is (= 10 (.getMaxTotal cfg)))
      (t/is (not (.getTestOnBorrow cfg)))))

  (t/testing "returning an instance as-is"
    (let [cfg (doto (GenericObjectPoolConfig.)
                (.setMaxTotal 5))]
      (t/is (identical? cfg (#'rq/resolve-pool-config cfg)))))

  (t/testing "skip configuration"
    (t/is (nil? (#'rq/resolve-pool-config :skip)))))

(t/deftest create-client-with-options-test
  (t/testing "create-client accepts option map"
    (let [client (rq/create-client "redis://localhost:6379"
                                   {:pool-config :skip})]
      (t/is (instance? clojure.lang.Atom client))
      (rq/close-client client))))


