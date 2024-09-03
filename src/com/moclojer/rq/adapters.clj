(ns com.moclojer.rq.adapters
  (:require
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.tools.logging :as log])
  (:import
   [redis.clients.jedis.args ListPosition]))

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
  (log/debug :packing pattern queue-name)
  (str (pattern->str pattern) queue-name))

(defn unpack-pattern
  [pattern queue-name]
  (log/debug :unpacking pattern queue-name)
  (subs queue-name (count (pattern->str pattern))))

(defn- keyword-enc->fn
  [enc]
  (let [fns {:none identity
             :edn pr-str
             :json json/write-str
             :array #(into-array (map pr-str %))
             :edn-array #(into-array (map pr-str %))
             :json-array #(into-array (map json/write-str %))}]
    (or (get fns enc)
        (throw (ex-info (str "No encoding " (name enc))
                        {:cause :illegal-argument
                         :value enc
                         :expected (keys fns)})))))

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

(defn- keyword-dec->fn
  [dec]
  (let [json-dec-fn #(json/read-str % :key-fn keyword)
        array? #(or (seq? %)
                    (some-> % class .isArray)
                    (instance? java.util.ArrayList %))
        fns {:none identity
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
                            [(json-dec-fn %)])}]
    (or (get fns dec)
        (throw (ex-info (str "No decoding " (name dec))
                        {:cause :illegal-argument
                         :value dec
                         :expected (keys fns)})))))

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
