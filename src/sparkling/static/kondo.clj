(ns sparkling.static.kondo
  (:require [clojure.java.shell :refer [sh]]
            [promesa.core :as p]
            [sparkling.builders.util :refer [parse-edn]]))

(defn- analyze-sync [& args]
  (->> (apply sh
              "clj-kondo" "--cache"
              "--config" (str {:output {:analysis true :format :edn}})
              args)
       :out
       parse-edn
       :analysis))

(defn- analyze-path-sync [path]
  (analyze-sync "--lint" path))

(defn- analyze-string-sync [s]
  (analyze-sync "--lint" "-" :in s))

(defn analyze-path [path]
  (p/future (analyze-path-sync path)))

(defn analyze-string [s]
  (p/future (analyze-string-sync s)))
