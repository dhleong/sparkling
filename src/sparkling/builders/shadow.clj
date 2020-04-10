(ns sparkling.builders.shadow
  (:require [clojure.edn :as edn]
            [clojure.java.io :refer [file]]))

(def parse-config (partial edn/read-string {:default #(do %2)}))

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
       parse-config))

(defn fill-project-config [project-config]
  (when-let [shadow (try (read-config project-config)
                         (catch Throwable _
                           nil))]
    (assoc project-config :source-paths (:source-paths shadow))))
