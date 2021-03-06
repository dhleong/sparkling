(ns sparkling.builders.shadow
  (:require [clojure.java.io :refer [file]]
            [sparkling.builders.util :refer [parse-edn]]
            [sparkling.config.util :refer [add-source-paths]]))

(defn detect-nrepl-port [project-config]
  (slurp (file (:root-path project-config)
               ".shadow-cljs"
               "nrepl.port")))

(defn extract-builds [config]
  (->> config
       :builds
       keys))

(defn read-config [project-config]
  (->> (file (:root-path project-config)
             "shadow-cljs.edn")
       (slurp)
       parse-edn))

(defn try-read-config [project-config]
  (try
    (read-config project-config)
    (catch Throwable _
      nil)))

(defn fill-project-config [project-config]
  (when-let [shadow (try-read-config project-config)]
    (add-source-paths project-config (:source-paths shadow))))
