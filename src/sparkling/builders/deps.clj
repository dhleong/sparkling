(ns sparkling.builders.deps
  (:require [clojure.java.io :refer [file]]
            [sparkling.builders.util :refer [parse-edn]]
            [sparkling.config.util :refer [add-source-paths]]))

(defn read-config [project-config]
  (->> (file (:root-path project-config)
             "deps.edn")
       (slurp)
       parse-edn))

(defn try-read-config [project-config]
  (try
    (read-config project-config)
    (catch Throwable _
      nil)))

(defn fill-project-config [project-config]
  (when-let [deps (try-read-config project-config)]
    (add-source-paths project-config (:paths deps))))
