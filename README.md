# clj-rq

RQ (Redis Queue) is a simple Clojure package for queueing jobs and processing them in the background with workers. It is backed by Redis and it is designed to have a low barrier to entry, inspired by [python-rq](https://python-rq.org).

> "simple is better than complex" - [The Zen of Python](https://peps.python.org/pep-0020/)


## exemple

```clojure
(ns rq.example
  (:require [clj-rq.core :as rq]
            [clj-rq.queue :as queue]
            [clj-rq.pubsub :as pubsub]))

(def redis-client (rq/redis-client "redis://localhost:6379/0"))

;; queue: type connect
(def q (queue/connection redis-client "my-queue"))

;; queue: rpush
(queue/producer q {:foo "bar"})
(queue/producer q {:foo "bar"} :at ... :in ... :retry 3 :retry-delay 10)

;; queue: blpop
(queue/consumer q (fn [job] (println job))

;; pub/sub
(pubsub/publish redis-client "name-subs" "value set")
(pubsub/subscribe redis-client ["name-subs"])
```
