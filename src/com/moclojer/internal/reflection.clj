(ns com.moclojer.internal.reflection
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
   [com.moclojer.rq.utils :as utils]))

(defn unpack-parameter
  [parameter]
  {:type (.. parameter getType getName)
   :name (csk/->kebab-case (.getName parameter))})

(defn unpack-method
  [method]
  {:name (csk/->kebab-case (.getName method))
   :parameters (map unpack-parameter (.getParameters method))})

(defn reduce-method-overloads
  [methods]
  (reduce
   (fn [overloaded-methods {:keys [name parameters]}]
     (let [overload-count (count
                           (filter
                            #(str/starts-with? (key %) name)
                            overloaded-methods))
           cur-overload-id (when (> overload-count 0) overload-count)]
       (assoc overloaded-methods (str name cur-overload-id) parameters)))
   {} methods))

(defn get-klazz-methods
  [klazz allowlist]
  (->> (.getMethods klazz)
       (map unpack-method)
       (filter #(contains? allowlist (:name %)))
       (reduce-method-overloads)))

(defmacro ->wrap-method
  [method parameters]
  (let [wrapped-method (clojure.string/replace method #"[`0-9]" "")
        param-syms (map #(-> % :name symbol) parameters)]
    `(defn ~(symbol method)
       ~(str "Wraps redis.clients.jedis.JedisPooled." wrapped-method)

       ~(-> (into ['client] param-syms)
            (conj '& 'options))

       (let [~{:keys ['pattern 'encoding 'decoding]
               :or {'pattern :rq
                    'encoding :edn
                    'decoding :edn}} ~'options
             ~'result ~(seq
                        (into
                         [(symbol (str "." wrapped-method)) '@client]
                         (reduce
                          (fn [acc par]
                            (->> (cond
                                   (= par 'key)
                                   `(com.moclojer.rq.utils/pack-pattern
                                     ~'pattern ~par)

                                   (= par 'value)
                                   `(com.moclojer.rq.utils/encode
                                     ~'encoding ~par)

                                   :else par)
                                 (conj acc)))
                          []
                          param-syms)))]
         (try
           (com.moclojer.rq.utils/decode ~'decoding ~'result)
           (catch ~'Exception ~'e
             (.printStackTrace ~'e)
             ~'result))))))

(comment
  (let [[method parameters] (-> redis.clients.jedis.JedisPooled
                                (get-klazz-methods #{"lpush"})
                                first)]
    (clojure.pprint/pprint
     (macroexpand-1 `(->wrap-method ~method ~parameters))))
  ;;
  )
