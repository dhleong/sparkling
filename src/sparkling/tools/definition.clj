(ns sparkling.tools.definition
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.apropos :refer [static-apropos]]
            [sparkling.static.kondo :as kondo]
            [sparkling.tools.util :refer [extract-symbol-input]]
            [sparkling.util.promise :as promise]))

(defn kondo-definitions [{:keys [document-text
                                 character
                                 line]
                          :as ctx}]
  (p/let [config *project-config*
          {:keys [match]} (extract-symbol-input
                            document-text character line)
          _ (println "match= " match)
          results (static-apropos (assoc ctx :root-path (:root-path config))
                                  match)]
    (->> results
         (keep :sparkling/definition))))

(defn kondo->location [{:keys [uri]} config {:keys [filename row col]}]
  {:uri (if (= "<stdin>" filename)
          uri
          (kondo/resolve-uri config filename))
   :line (dec row)
   :col col})

(defn- find-definition-kondo [ctx]
  (p/plet [config *project-config*
          definitions (kondo-definitions ctx)]
    (->> definitions
         (map (partial kondo->location ctx config))
         distinct)))

(def ^{:doc "Returns a promise that resolves to a sequence of locations,
             with keys :uri, :line, :col"}
  find-definition
  (promise/fallback
    find-definition-kondo))
