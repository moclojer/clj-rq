(ns com.moclojer.tools.build
  (:refer-clojure :exclude [test])
  (:require
   [clojure.string :as string]
   [clojure.tools.build.api :as b]
   [com.moclojer.rq :as rq]))

(def class-dir "target/classes")
(def jar-file "target/com.moclojer.rq.jar")

(set! *warn-on-reflection* true)

(defmacro with-err-str
  [& body]
  `(let [s# (new java.io.StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(def pom-template
  [[:description "RQ (Redis Queue) is a simple Clojure package for queueing jobs and processing them in the background with workers. It is backed by Redis and it is designed to have a low barrier to entry"]
   [:url "https://github.com/moclojer/clj-rq"]
   [:licenses
    [:license
     [:name "MIT License"]
     [:url "https://opensource.org/licenses/MIT"]]]
   [:scm
    [:url "https://github.com/moclojer/clj-rq"]
    [:connection "scm:git:https://github.com/moclojer/clj-rq.git"]
    [:developerConnection "scm:git:ssh:git@github.com:moclojer/clj-rq.git"]
    [:tag (str "v" rq/version)]]])

(def options
  (let [basis (b/create-basis {:project "deps.edn"})]
    {:class-dir  class-dir
     :lib        'com.moclojer/rq
     :main       'com.moclojer.rq
     :version    rq/version
     :basis      basis
     :ns-compile '[com.moclojer.rq com.moclojer.rq.queue]
     :uber-file  jar-file
     :jar-file   jar-file
     :target     "target"
     :src-dirs   (:paths basis)
     :pom-data   pom-template
     :exclude    ["docs/*" "test/*" "target/*"]}))

(defn -main
  [& _]
  (let [basis (b/create-basis {:project "deps.edn"})]
    (println "Clearing target directory")
    (b/delete {:path "target"})

    (println "Writing pom")
    (->> (b/write-pom options)
         with-err-str
         string/split-lines
         ;; Avoid confusing future me/you: suppress "Skipping coordinate" messages for our jars, we don't care, we are creating an uberjar
         (remove #(re-matches #"^Skipping coordinate: \{:local/root .*target/(lib1|lib2|graal-build-time).jar.*" %))
         (run! println))
    (b/copy-dir {:src-dirs (:paths basis)
                 :target-dir class-dir})

    (println "Compile sources to classes")
    (b/compile-clj options)

    (println "Packaging classes into jar")
    (b/jar options)))
