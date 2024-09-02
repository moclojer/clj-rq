(ns com.moclojer.test-helpers)

(defn gen-message
  "Generates a fuzzy message"
  []
  {(random-uuid) 1
   (keyword (str (random-uuid))) true
   :test 'hello
   :my/test2 "123"
   :foobar ["321"]})
