(ns com.moclojer.rq.adapters
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.tools.logging :as log])
  (:import
   [redis.clients.jedis.args ListPosition]))

(def patterns
  {:none ""
   :rq "rq:"
   :pubsub "rq:pubsub:"
   :pending "rq:pubsub:pending:"})

(defn- pattern->str
  "Adapts given pattern keyword to a know internal pattern. Raises
  an exception if invalid."
  [pattern]
  (or (get-in patterns [pattern])
      (throw (ex-info (str "No pattern named " pattern)
                      {:cause :illegal-argument
                       :value pattern
                       :expected (keys patterns)}))))

(defn pack-pattern
  [pattern queue-name]
  (log/debug :packing pattern queue-name)
  (str (pattern->str pattern) queue-name))

(defn unpack-pattern
  [pattern queue-name]
  (log/debug :unpacking pattern queue-name)
  (let [prefix (pattern->str pattern)]
    (if (str/starts-with? queue-name prefix)
      (subs queue-name (count prefix))
      (do
        (log/warn :invalid-prefix
                  :queue-name queue-name
                  :expected-prefix prefix)
        queue-name))))

(def encoding-fns
  {:none identity
   :edn pr-str
   :json json/write-str
   :array #(into-array (map pr-str %))
   :edn-array #(into-array (map pr-str %))
   :json-array #(into-array (map json/write-str %))})

(defn- keyword-enc->fn
  [enc]
  (or (get encoding-fns enc)
      (throw (ex-info (str "No encoding " (name enc))
                      {:cause :illegal-argument
                       :value enc
                       :expected (set (keys encoding-fns))}))))

(defn encode
  [enc message]
  (log/debug :encoding enc message)
  ((cond
     (keyword? enc) (keyword-enc->fn enc)
     (fn? enc) enc
     :else (throw (ex-info
                   (str "`encoding` must be either keyword or function")
                   {:cause :illegal-argument
                    :value enc
                    :expected #{keyword? fn?}})))
   message))

(def decoding-fns
  (let [json-dec-fn #(json/read-str % :key-fn keyword)
        array? #(or (seq? %)
                    (some-> % class .isArray)
                    (instance? java.util.ArrayList %))]
    {:none identity
     :edn edn/read-string
     :json json-dec-fn
     :array #(if (array? %)
               (vec %)
               [%])
     :edn-array #(if (array? %)
                   (vec (map edn/read-string %))
                   [(edn/read-string %)])
     :json-array #(if (array? %)
                    (vec (map json-dec-fn %))
                    [(json-dec-fn %)])}))

(defn- keyword-dec->fn
  [dec]
  (or (get decoding-fns dec)
      (throw (ex-info (str "No decoding " (name dec))
                      {:cause :illegal-argument
                       :value dec
                       :expected (set (keys decoding-fns))}))))

(defn decode
  [dec message]
  (log/debug :decoding dec message)
  ((cond
     (keyword? dec) (keyword-dec->fn dec)
     (fn? dec) dec
     :else (throw (ex-info
                   (str "`decoding` must be either keyword or function")
                   {:cause :illegal-argument
                    :value dec
                    :expected #{keyword? fn?}})))
   message))

(defn ->list-position
  [pos]
  (or (get {:before ListPosition/BEFORE
            :after ListPosition/AFTER}
           pos)
      (throw (ex-info (str "No list position named " pos)
                      {:cause :illegal-argument
                       :value pos
                       :expected #{:before :after}}))))
