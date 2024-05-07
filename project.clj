(defproject com.moclojer/clj-rq "0.1.0"
  :author "moclojer <https://www.moclojer.com>"
  :description "RQ (Redis Queue) is a simple Clojure package for queueing jobs and processing them in the background with workers. It is backed by Redis and it is designed to have a low barrier to entry"
  :url "https://github.com/moclojer/clj-rq"

  :license
  {:name "MIT"
   :url  "http://opensource.org/licenses/MIT"}

  :scm {:name "git" :url "https://github.com/moclojer/clj-rq"}

  :test-paths ["test" #_"src"]

  :dependencies
  [[redis.clients/jedis "5.1.2"]]

    :plugins
    [[lein-pprint  "1.3.2"]
     [lein-ancient "0.7.0"]
     [com.taoensso.forks/lein-codox "0.10.11"]])
