(ns com.moclojer.internal.reflection
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn unpack-parameter
  [parameter]
  {:type (-> (.. parameter getType getName)
             (str/split #"\.")
             (last)
             (csk/->kebab-case))
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

(def ^:private jedis-cmd-allowlist
  (-> (io/resource "command-allowlist.edn")
      (slurp)
      (edn/read-string)))

(comment
  (get-klazz-methods
   redis.clients.jedis.JedisPooled
   jedis-cmd-allowlist)
  ;;
  )
