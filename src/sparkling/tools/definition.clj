(ns sparkling.tools.definition
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.apropos :refer [static-apropos]]
            [sparkling.tools.util :refer [extract-query-input]]
            [sparkling.util.promise :as promise]))

(defn- resolve-kondo-filename [ctx config filename]
  (println "ctx=" ctx)
  (println "config=" config)
  ; TODO kondo tends to give relative filenames...
  filename)

(defn- find-definition-kondo [{:keys [document-text
                                      uri
                                      character
                                      line]
                               :as ctx}]
  (p/let [config *project-config*
          {:keys [match]} (extract-query-input
                          document-text character line)
          results (static-apropos (assoc ctx :root-path (:root-path config))
                                  match)]
    (->> results
         (map (fn [{{:keys [filename row col]} :sparkling/definition}]
                {:uri (if (= "<stdin>" filename)
                        uri
                        (resolve-kondo-filename ctx config filename))
                 :line (dec row)
                 :col col})))))

(def ^{:doc "Returns a promise that resolves to a sequence of locations,
             with keys :filename, :line, :col"}
  find-definition
  (promise/fallback
    find-definition-kondo))
