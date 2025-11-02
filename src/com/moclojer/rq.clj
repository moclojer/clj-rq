(ns com.moclojer.rq
  (:import
   [java.net URI URISyntaxException]
   [org.apache.commons.pool2.impl GenericObjectPoolConfig]
   [redis.clients.jedis JedisPooled]))

(def version "0.2.2")

;; redis connection pool to be thread safe
(def
  ^{:private true :dynamic true}
  *redis-pool* (ref nil))

(def ^:private default-pool-settings
  {:max-total 128
   :max-idle 128
   :min-idle 16
   :max-wait-ms 1000
   :test-on-borrow true
   :test-on-return true
   :test-on-create true
   :test-while-idle true
   :min-evictable-idle-ms 60000
   :soft-min-evictable-idle-ms 60000
   :time-between-eviction-runs-ms 30000
   :num-tests-per-eviction-run 3
   :block-when-exhausted true})

(def ^:private pool-config-setters
  {:max-total (fn [^GenericObjectPoolConfig cfg v]
                (.setMaxTotal cfg (int v)))
   :max-idle (fn [^GenericObjectPoolConfig cfg v]
               (.setMaxIdle cfg (int v)))
   :min-idle (fn [^GenericObjectPoolConfig cfg v]
               (.setMinIdle cfg (int v)))
   :max-wait-ms (fn [^GenericObjectPoolConfig cfg v]
                  (.setMaxWaitMillis cfg (long v)))
   :test-on-borrow (fn [^GenericObjectPoolConfig cfg v]
                     (.setTestOnBorrow cfg (boolean v)))
   :test-on-return (fn [^GenericObjectPoolConfig cfg v]
                     (.setTestOnReturn cfg (boolean v)))
   :test-on-create (fn [^GenericObjectPoolConfig cfg v]
                     (.setTestOnCreate cfg (boolean v)))
   :test-while-idle (fn [^GenericObjectPoolConfig cfg v]
                      (.setTestWhileIdle cfg (boolean v)))
   :min-evictable-idle-ms (fn [^GenericObjectPoolConfig cfg v]
                            (.setMinEvictableIdleTimeMillis cfg (long v)))
   :soft-min-evictable-idle-ms (fn [^GenericObjectPoolConfig cfg v]
                                 (.setSoftMinEvictableIdleTimeMillis cfg (long v)))
   :time-between-eviction-runs-ms (fn [^GenericObjectPoolConfig cfg v]
                                    (.setTimeBetweenEvictionRunsMillis cfg (long v)))
   :num-tests-per-eviction-run (fn [^GenericObjectPoolConfig cfg v]
                                 (.setNumTestsPerEvictionRun cfg (int v)))
   :eviction-policy-class-name (fn [^GenericObjectPoolConfig cfg v]
                                 (.setEvictionPolicyClassName cfg (str v)))
   :block-when-exhausted (fn [^GenericObjectPoolConfig cfg v]
                           (.setBlockWhenExhausted cfg (boolean v)))
   :fairness (fn [^GenericObjectPoolConfig cfg v]
               (.setFairness cfg (boolean v)))
   :lifo (fn [^GenericObjectPoolConfig cfg v]
           (.setLifo cfg (boolean v)))
   :jmx-enabled (fn [^GenericObjectPoolConfig cfg v]
                  (.setJmxEnabled cfg (boolean v)))
   :jmx-name-base (fn [^GenericObjectPoolConfig cfg v]
                    (.setJmxNameBase cfg (str v)))
   :jmx-name-prefix (fn [^GenericObjectPoolConfig cfg v]
                      (.setJmxNamePrefix cfg (str v)))
   :evictor-shutdown-timeout-ms (fn [^GenericObjectPoolConfig cfg v]
                                  (.setEvictorShutdownTimeoutMillis cfg (long v)))})

(defn- build-pool-config
  "Return a GenericObjectPoolConfig instance configured with the supplied settings map.
  Throws when unknown keys are passed."
  [settings]
  (when (seq settings)
    (let [cfg (GenericObjectPoolConfig.)]
      (doseq [[k v] settings]
        (when (some? v)
          (if-let [setter (get pool-config-setters k)]
            (setter cfg v)
            (throw (ex-info (str "Unsupported pool configuration key: " k)
                            {:cause :invalid-pool-config-key
                             :key k
                             :allowed (set (keys pool-config-setters))})))))
      cfg)))

(defn- resolve-pool-config
  "Accepts nil, a map of settings, :skip, or an existing GenericObjectPoolConfig instance."
  [pool-config]
  (cond
    (instance? GenericObjectPoolConfig pool-config) pool-config
    (= :skip pool-config) nil
    (map? pool-config) (let [{:keys [inherit-defaults?] :or {inherit-defaults? true}} pool-config
                             settings (dissoc pool-config :inherit-defaults?)]
                         (build-pool-config
                          (if inherit-defaults?
                            (merge default-pool-settings settings)
                            settings)))
    (nil? pool-config) (build-pool-config default-pool-settings)
    :else (throw (ex-info "Pool configuration must be nil, :skip, a map or GenericObjectPoolConfig instance."
                          {:cause :invalid-pool-config
                           :value pool-config}))))

(defn- create-pool
  [url pool-config]
  (let [cfg (resolve-pool-config pool-config)]
    (try
      (if cfg
        (JedisPooled. cfg (URI. url))
        (JedisPooled. url))
      (catch URISyntaxException e
        (throw (ex-info (str "Invalid Redis URL: " url)
                        {:cause :invalid-redis-url
                         :url url}
                        e))))))

(defn create-client
  "Connect to redis client.
   If `ref?` (or `:ref?` in the options map) is true, store the created instance
   in the global var `*redis-pool*`. Otherwise, returns an atom holding the created instance.
   Pool configuration can be provided through `pool-config` or `{:pool-config ...}`."
  ([url]
   (create-client url false nil))
  ([url ref-or-opts]
   (if (map? ref-or-opts)
     (let [allowed #{:ref? :pool-config}
           unknown (seq (remove allowed (keys ref-or-opts)))]
       (when unknown
         (throw (ex-info "Unsupported option keys provided to create-client."
                         {:cause :invalid-create-client-options
                          :unknown (set unknown)
                          :allowed allowed})))
       (create-client url
                      (boolean (:ref? ref-or-opts))
                      (:pool-config ref-or-opts)))
     (create-client url ref-or-opts nil)))
  ([url ref? pool-config]
   (let [pool (create-pool url pool-config)
         store? (boolean ref?)]
     (if (and store? (not @*redis-pool*))
       (dosync
        (ref-set *redis-pool* pool)
        *redis-pool*)
       (atom pool)))))

(defn close-client
  "Disconnect and close redis client.
   If no specific client is passed, the global client stored is closed;"
  ([] (close-client *redis-pool*))
  ([client] (.close @client)))
