(ns sparkling.static.classpath
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [promesa.core :as p]))

(defn- classpath-deps [project-root]
  (->> (sh "clojure" "-Spath"
           :dir project-root)
       :out
       str/trim))

(defn classpath-at [project-root]
  (println "Analyzing classpath at " project-root "...")
  (p/future
    ; TODO other tools (lein, boot, shadow...)
    (classpath-deps project-root)))
