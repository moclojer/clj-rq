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
  [method parameters allowlist]
  (let [wrapped-method (clojure.string/replace method #"[`0-9]" "")
        base-doc (str "Wraps redis.clients.jedis.JedisPooled."
                      wrapped-method)
        param-syms (map #(-> % :name symbol) parameters)
        [doc enc dec] (get allowlist method ["" :none :none])]
    `(defn ~(symbol method)
       ~(str base-doc \newline doc)

       ~(-> (into ['client] param-syms)
            (conj '& 'options))

       (let [~{:keys ['pattern 'encoding 'decoding]
               :or {'pattern :rq
                    'encoding enc
                    'decoding dec}} ~'options

             ~'result ~(->> (reduce
                             (fn [acc par]
                               (->> (cond
                                      (= par 'key)
                                      `(com.moclojer.rq.utils/pack-pattern
                                        ~'pattern ~par)

                                      (some #{'value 'string 'args} [par])
                                      `(com.moclojer.rq.utils/encode
                                        ~'encoding ~par)

                                      :else par)
                                    (conj acc)))
                             []
                             param-syms)
                            (into [(symbol (str "." wrapped-method)) '@client])
                            (seq))]
         (try
           (com.moclojer.rq.utils/decode ~'decoding ~'result)
           (catch ~'Exception ~'e
             (.printStackTrace ~'e)
             ~'result))))))

(comment
  (let [allowlist {"lpush" ["hello" :edn-array :none]}
        [method parameters] (-> redis.clients.jedis.JedisPooled
                                (get-klazz-methods (set (keys allowlist)))
                                first)]
    (clojure.pprint/pprint
     (macroexpand-1 `(->wrap-method ~method ~parameters ~allowlist))))

  ;;
  )
