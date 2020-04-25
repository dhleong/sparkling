(ns sparkling.nrepl.detect
  (:require [clojure.java.io :refer [file]]
            [sparkling.builders.shadow :as shadow]))

(defn- nrepl [project-config]
  [:nrepl (slurp (file (:root-path project-config)
                       ".nrepl-port"))])

(defn- shadow [project-config]
  [:shadow (shadow/detect-nrepl-port project-config)])

(defn- try-detect [method config]
  (try
    (method config)
    (catch Exception e
      (println method " not found: " e)
      nil)))

(defn nrepl-port [project-config]
  (let [[source p] (or (try-detect nrepl project-config)
                       (try-detect shadow project-config))]
    (if p
      [source (cond
                (int? p) p
                (string? p) (Integer/parseInt p)
                :else (throw (ex-info "Invalid port"
                                      {:config project-config
                                       :port p})))]

      (throw (ex-info "Could not detect nrepl"
                      {:config project-config})))))

(defn shadow-builds [project-config]
  (some->> project-config
           shadow/read-config
           shadow/extract-builds))
