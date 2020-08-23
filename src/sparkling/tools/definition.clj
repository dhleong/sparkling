(ns sparkling.tools.definition
  (:require [clojure.java.io :as io]
            [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.apropos :refer [static-apropos]]
            [sparkling.tools.util :refer [extract-symbol-input]]
            [sparkling.util.promise :as promise]))

(defn- resolve-kondo-uri [ctx config filename]
  (println "ctx=" ctx)
  (println "config=" config)
  (println "filename=" filename)

  (when-let [path (or (when (.isAbsolute (io/file filename))
                        filename)

                      (let [from-root (io/file (:root-path config)
                                               filename)]
                        (when (.exists from-root)
                          (.getAbsolutePath from-root)))

                      filename)]
    (str "file://" path)))

(defn- find-definition-kondo [{:keys [document-text
                                      uri
                                      character
                                      line]
                               :as ctx}]
  (p/let [config *project-config*
          {:keys [match]} (extract-symbol-input
                            document-text character line)
          results (static-apropos (assoc ctx :root-path (:root-path config))
                                  match)]
    (println "query: " match)
    (println "results: " (map :candidate results))
    (->> results
         (keep :sparkling/definition)
         (map (fn [{:keys [filename row col]}]
                {:uri (if (= "<stdin>" filename)
                        uri
                        (resolve-kondo-uri ctx config filename))
                 :line (dec row)
                 :col col})))))

(def ^{:doc "Returns a promise that resolves to a sequence of locations,
             with keys :filename, :line, :col"}
  find-definition
  (promise/fallback
    find-definition-kondo))
