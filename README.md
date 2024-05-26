# clj-rq

RQ (Redis Queue) is a simple Clojure package for queueing jobs and processing them in the background with workers. It is backed by Redis and it is designed to have a low barrier to entry, inspired by [python-rq](https://python-rq.org).

> "simple is better than complex" - [The Zen of Python](https://peps.python.org/pep-0020/)

## exemple

```clojure
(ns rq.example
  (:require [clj-rq.rq :as rq]
            [clj-rq.queue :as queue]
            [clj-rq.pubsub :as pubsub]))

(def *redis-pool* (rq/client "redis://localhost:6379/0"))

;; queue
(queue/producer *redis-pool* "my-queue" {:now (java.time.LocalDateTime/now)
                                            :foo "bar"})
(println :size (queue/consumer-size *redis-pool* "my-queue"))
(queue/consumer *redis-pool* "my-queue" #(prn :msg %1))

;; pub/sub
(pubsub/publish *redis-pool* "name-subs" "value set")
(pubsub/subscribe *redis-pool* #(prn :chan %1 :msg %2) ["name-subs"])
```
