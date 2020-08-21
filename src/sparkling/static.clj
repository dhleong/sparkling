(ns sparkling.static
  (:require [promesa.core :as p]
            [systemic.core :refer [defsys]]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.classpath :refer [classpath-at]]
            [sparkling.static.kondo :as kondo]))

(defn- load-kondo-classpath [config-promise]
  (p/let [config config-promise
          cp (classpath-at (:root-path config))]
    (println "Analyzing kondo classpath for" config ": " cp)
    (kondo/analyze-path (:root-path config) cp)))

(defsys *kondo-classpath*
  "A promise that resolves to a clj-kondo-powered analysis
   of the project classpath"
  :deps [*project-config*]
  :start (load-kondo-classpath *project-config*))

(defsys *kondo-project-path*
  "A promise that resolves to a clj-kondo-powered analysis
   of the project root"
  :deps [*project-config*]
  :start (p/let [config *project-config*]
           (kondo/analyze-path (:root-path config) (:root-path config))))
