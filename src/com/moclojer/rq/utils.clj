(ns com.moclojer.rq.utils)

(defn- pattern->str
  [pattern]
  (let [patterns {:rq "rq:"
                  :pending "rq:pending:"}]
    (or (get-in patterns [pattern])
        (throw (ex-info (str "No pattern named " pattern)
                        {:cause :illegal-argument
                         :value pattern
                         :expected (keys patterns)})))))

(defn pack-pattern
  [pattern queue-name]
  (str (pattern->str pattern) queue-name))

(defn unpack-pattern
  [pattern queue-name]
  (apply str (-> (pattern->str pattern)
                 (count)
                 (drop queue-name))))

(comment
  (pack-pattern :rq "my-queue")
  ;; => "rq:my-queue"

  (pack-pattern :pending "foobar")
  ;; => "rq:pendingfoobar"

  (unpack-pattern :pending "rq:pending:foobar")
  ;; => "foobar"
  )
