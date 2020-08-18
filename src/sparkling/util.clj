(ns sparkling.util
  (:require [clojure.java.io :as io]
            [promesa.core :as p]
            [sparkling.builders.util :refer [parse-edn]]))

(def version (memoize
               #(or (some-> (io/resource "deps.edn")
                            slurp
                            parse-edn
                            :version)
                    "SNAPSHOT")))

(defn fallback
  "Given a variable number of fns, each of which is expected to
   return a Promise, returns a fn that calls each, in order, falling
   back to the next if the promise returned from the one before rejected."
  [& fns]
  (if (= 1 (count fns))
    (first fns)

    (fn fallback-fn [& args]
      (reduce
        (fn [p next-fn]
          (p/catch p (fn [e]
                       (println "nrepl WARN: " e)
                       (apply next-fn args))))

        (apply (first fns) args)
        (next fns)))))
