(ns sparkling.tools.references
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static :refer [*kondo-project-path*]]
            [sparkling.static.kondo :as kondo]
            [sparkling.tools.definition :refer [kondo-definitions
                                                kondo->location]]
            [sparkling.util.promise :as promise]))

(defn- definitions->symbols [defs]
  (->> defs
       (map (fn [d]
              (symbol (name (:ns d))
                      (name (:name d)))))
       (into #{})))

(defn- xf-references-to [ctx config defs]
  (comp
    (filter (fn [usage]
              (let [s (symbol (name (:to usage))
                              (name (:name usage)))]
                (contains? defs s))))
    (map (partial kondo->location ctx config))))

(defn- find-references-kondo [{:keys [document-text] :as ctx}]
  (p/plet [config *project-config*
           defs (-> (promise/with-timing "kondo-definitions"
                      (kondo-definitions ctx))
                    (p/then definitions->symbols))
           local-analysis (kondo/analyze-string document-text)
           project *kondo-project-path*]
    (->> (concat (:var-usages project)
                 (:var-usages local-analysis))
         (transduce (xf-references-to ctx config defs) conj [])
         distinct)))


(def ^{:doc "Returns a promise that resolves to a sequence of locations,
             with keys :uri, :line, :col"}
  find-references
  (promise/fallback
    find-references-kondo))
