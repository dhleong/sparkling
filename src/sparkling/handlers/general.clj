(ns sparkling.handlers.general
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.spec :as spec]
            [sparkling.spec.util :refer [validate]]))

(defhandler :initialize [{:keys [_clientInfo capabilities rootPath]}]
  (println ":initialize" capabilities)

  (let [config (validate ::spec/project-config
                         {:root-path rootPath})]
    (p/resolve! *project-config* config))

  {:capabilities
   {:textDocumentSync {:openClose false

                       ; 1 = sync the full file
                       ; 2 = incremental
                       :change 1}}

   :serverInfo {:name "sparkling"
                :version "SNAPSHOT"}})

(defhandler :initialized [params]
  (println "Client initialized!" params))
