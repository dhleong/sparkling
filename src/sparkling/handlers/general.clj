(ns sparkling.handlers.general
  (:require [promesa.core :as p]
            [systemic.core :as systemic]
            [sparkling.config :refer [*project-config*]]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.lsp :refer [*lsp*]]
            [sparkling.nrepl :refer [*nrepl*]]
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

(defhandler :shutdown []
  (println "Shutdown request received")
  (systemic/stop! `*project-config* `*nrepl*))

(defhandler :exit []
  (println "Exit request received")
  (if (systemic/state `*lsp*)
    ; lsp is running, which means we have not yet
    ; received a :shutdown request
    (System/exit 1)

    ; we received :shutdown
    (System/exit 0)))
