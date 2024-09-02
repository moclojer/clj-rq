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
  [arities methods]
  (reduce
   (fn [underloaded-methods {:keys [name parameters]}]
     (if (= (count parameters) (get arities name))
       (assoc underloaded-methods name parameters)
       underloaded-methods))
   {} methods))

(defn get-klazz-methods
  [klazz allowmap]
  (let [allowlist (set (keys allowmap))
        arities (reduce-kv
                 (fn [acc k v]
                   (assoc acc k (first v)))
                 {} allowmap)]
    (->> (.getMethods klazz)
         (map unpack-method)
         (filter #(contains? allowlist (:name %)))
         (underload-methods arities))))

(defmacro ->wrap-method
  [method parameters allowmap]
  (let [wrapped-method (clojure.string/replace method #"[`0-9]" "")
        base-doc (str "Wraps redis.clients.jedis.JedisPooled." wrapped-method)
        param-syms (map #(-> % :name symbol) parameters)
        [_ doc enc dec] (get allowmap method [nil "" :none :none])]
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

                                      (some #{'value 'string 'args} [par])
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
  (require '[clojure.pprint :refer [pprint]])

  (get-klazz-methods
   redis.clients.jedis.JedisPooled
   {"rpop" [2 "hello" :edn-array :none]})

  (let [allowmap {"rpop" [2 "hello" :edn-array :none]}
        [method parameters] (first
                             (get-klazz-methods
                              redis.clients.jedis.JedisPooled
                              allowmap))]
    (pprint
     (macroexpand-1 `(->wrap-method ~method ~parameters ~allowmap))))

  ;;
  )
