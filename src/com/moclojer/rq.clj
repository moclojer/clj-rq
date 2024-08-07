(ns com.moclojer.rq
  (:import
   [redis.clients.jedis JedisPooled]))

(def version "0.1.4")

;; redis connection pool to be thread safe
(def
  ^{:private true :dynamic true}
  *redis-pool* (ref nil))

(defn create-client
  "Connect to redis client. If `ref?` is true, will save the created instance
  in the global var `*redis-pool*. Just returns the created instance otherwise."
  ([url]
   (create-client url false))
  ([url ref?]
   (let [pool (JedisPooled. url)]
     (if (and ref? (not @*redis-pool*))
       (dosync
        (ref-set *redis-pool* pool)
        *redis-pool*)
       (atom pool)))))

(defn close-client
  "Disconnect and close redis client"
  ([] (close-client *redis-pool*))
  ([client] (.close @client)))

(comment
  ;; (import [java.util Arrays])
  (require '[clojure.string :as str]
           '[clojure.java.io :as io]
           '[clojure.edn :as edn]
           '[camel-snake-kebab.core :as csk])
  (import [redis.clients.jedis JedisPooled])

  (def allowlist
    (-> (io/resource "command-allowlist.edn")
        (slurp)
        (edn/read-string)))

  (->> (.getMethods JedisPooled)
       (map
        (fn [class]
          {:name (csk/->kebab-case (.getName class))
           :parameters (map
                        #(identity
                          {:type (-> (.. % getType getName)
                                     (str/split #"\.")
                                     (last)
                                     (csk/->kebab-case))
                           :name (csk/->kebab-case (.getName %))})
                        (.getParameters class))}))
       (filter #(contains? allowlist (:name %)))
       (reduce
        (fn [methods {:keys [name parameters]}]
          (let [overload-count (count
                                (filter
                                 #(str/starts-with? (key %) name)
                                 methods))
                cur-overload-id (when (> overload-count 0) overload-count)]
            (assoc methods (str name cur-overload-id) parameters)))
        {}))
  ;;
  )
