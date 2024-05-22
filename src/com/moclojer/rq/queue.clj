(ns com.moclojer.rq.queue)

(defn pattern-name
  [name]
  (str "rq:" name))

;; queue: rpush
;; (queue/producer client queue-name {:foo "bar"} :at ... :in ... :retry 3 :retry-delay 10)
(defn producer
  "Push a job to the queue"
  [redis-client queue-name message & {:keys [at in retry retry-delay]
                                      :or {retry 3 retry-delay 10}}]
  (println at in retry retry-delay)
  (.rpush @redis-client (pattern-name queue-name) (into-array [(pr-str message)])))

;; queue: blpop
;; (queue/consumer client queue-name (fn [job] (println job))
(defn consumer
  "Push a job to the queue"
  [redis-client queue-name timeout]
  (.blpop @redis-client timeout (pattern-name queue-name)))





