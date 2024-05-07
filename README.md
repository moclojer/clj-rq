# clj-rq

RQ (Redis Queue) is a simple Clojure package for queueing jobs and processing them in the background with workers. It is backed by Redis and it is designed to have a low barrier to entry, inspired by [python-rq](https://python-rq.org).

> "simple is better than complex" - [The Zen of Python](https://peps.python.org/pep-0020/)


## exemple

```clojure
(ns rq.example
  (:require [com.moclojer.rq.core :as rq]))

(def redis-client (rq/redis-client "redis://localhost:6379/0"))

(def q (rq/queue redis-client "my-queue"))

;; rpush
(rq/producer q {:foo "bar"})
(rq/producer q {:foo "bar"} :at ... :in ... :retry 3 :retry-delay 10)

;; blpop
(rq/consumer q (fn [job] (println job))
```
