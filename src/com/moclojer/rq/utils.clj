(ns com.moclojer.rq.utils
  (:require
   [clojure.string :as s]))

(defn- pattern->str
  "Adapts given pattern keyword to a know internal pattern. Raises
  an exception if invalid."
  [pattern]
  (let [patterns {:none ""
                  :rq "rq:"
                  :pubsub "rq:pubsub:"
                  :pending "rq:pubsub:pending:"}]
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
  (subs queue-name (count (pattern->str pattern))))



