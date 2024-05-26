(ns com.moclojer.rq.queue)

(defn pattern-name
  "Return the pattern name for the queue, which is the name prefixed with `rq:`"
  [name]
  (str "rq:" name))

;; queue:push
;; (queue/producer client queue-name {:foo "bar"} :at ... :in ... :retry 3 :retry-delay 10)
(defn producer
  "Push a job to the queue"
  [redis-client queue-name message & {:keys [direction at in retry retry-delay]
                                      :or {direction "l" retry 3 retry-delay 10}}]
  (println at in retry retry-delay)
  (let [qname (pattern-name queue-name)
        msg (into-array [(pr-str message)])]
    (if (= direction "r")
      (.rpush @redis-client qname msg)
      (.lpush @redis-client qname msg))))

(defn consumer-size
  "get size of the queue"
  [redis-client queue-name]
  (.llen @redis-client (pattern-name queue-name)))

;; queue:process and pop
;; (queue/consumer client queue-name (fn [job] (println job))
(defn consumer
  "Consume a message from the queue, blocking in loop until a message is available."
  [redis-client queue-name consumer-fn & {:keys [size loop-sleep direction]
                                          :or {direction "l" size -1 loop-sleep 0}}]
  (let [qname (pattern-name queue-name)]
    (loop []
      (Thread/sleep loop-sleep)
      (flush)
      (let [msg (if (= direction "r")
                  (.rrange @redis-client qname 0 size)
                  (.lrange @redis-client qname 0 size))]
        (when msg
          (consumer-fn msg)
          (println :size (consumer-size redis-client qname))
          (if (= direction "r")
            (.rpop @redis-client qname)
            (.lpop @redis-client qname))
          (System/exit 0)
          (recur))))))


