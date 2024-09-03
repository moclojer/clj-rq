(ns com.moclojer.internal.reflection
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.string :as str]
   [com.moclojer.rq.adapters :as adapters]))

(defn unpack-parameter
  [parameter]
  {:type (.. parameter getType getName)
   :name (csk/->kebab-case (.getName parameter))})

(defn unpack-method
  [method]
  {:name (csk/->kebab-case (.getName method))
   :parameters (map unpack-parameter (.getParameters method))})

(defn underload-methods
  [paramlist methods]
  (reduce
   (fn [underloaded-methods {:keys [name parameters]}]
     (let [allowed-params (get paramlist name)
           param-names (map :name parameters)]
       (if (and (= (count parameters) (count allowed-params))
                (every? #(some #{%} param-names) allowed-params))
         (assoc underloaded-methods name parameters)
         underloaded-methods)))
   {} methods))

(defn get-klazz-methods
  [klazz allowmap]
  (let [allowlist (set (keys allowmap))
        paramlist (reduce-kv
                   (fn [acc name method]
                     (assoc acc name (second method)))
                   {} allowmap)]
    (->> (.getMethods klazz)
         (map unpack-method)
         (filter #(contains? allowlist (:name %)))
         (underload-methods paramlist))))

(defmacro ->wrap-method
  [method parameters allowmap]
  (let [wrapped-method (clojure.string/replace method #"[`0-9]" "")
        base-doc (str "Wraps redis.clients.jedis.JedisPooled." wrapped-method)
        param-syms (map #(-> % :name symbol) parameters)
        [doc _ enc dec] (get allowmap method ["" nil :none :none])]
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
                                      `(com.moclojer.rq.adapters/pack-pattern
                                        ~'pattern ~par)

                                      (some #{'value 'string
                                              'args 'pivot} [par])
                                      `(com.moclojer.rq.adapters/encode
                                        ~'encoding ~par)

                                      :else par)
                                    (conj acc)))
                             []
                             param-syms)
                            (into [(symbol (str "." wrapped-method)) '@client])
                            (seq))]
         (try
           (com.moclojer.rq.adapters/decode ~'decoding ~'result)
           (catch ~'Exception ~'e
             (.printStackTrace ~'e)
             ~'result))))))

(comment
  (get-klazz-methods
   redis.clients.jedis.JedisPooled
   {"rpop" ["hello" ["key" "count"]  :edn-array :none]})

  (require '[clojure.pprint :refer [pprint]])
  (let [allowmap {"linsert"   ["Inserts a message into a queue in reference to given pivot"
                               ["key" "where" "pivot" "value"] :edn :none]}
        [method parameters] (first
                             (get-klazz-methods
                              redis.clients.jedis.JedisPooled
                              allowmap))]
    (pprint
     (macroexpand-1 `(->wrap-method ~method ~parameters ~allowmap))))

  ;;
  )
