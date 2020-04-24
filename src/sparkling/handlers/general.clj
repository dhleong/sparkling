(ns sparkling.handlers.general
  (:require [promesa.core :as p]
            [sparkling.util :as util]
            [systemic.core :as systemic]
            [sparkling.config :as config :refer [*project-config*]]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.lsp :refer [*lsp*]]
            [sparkling.nrepl :refer [*nrepl*]]))

(defhandler :initialize [{:keys [_clientInfo capabilities rootPath]}]
  (println ":initialize" capabilities)
  (systemic/start! `*project-config* `*nrepl*)

  (let [lsp {:did-save? (get-in capabilities [:textDocument
                                              :synchronization
                                              :didSave]
                                false)}
        config (config/fill {:root-path rootPath
                             :lsp lsp})]
    (p/resolve! *project-config* config)

    (when-not (:did-save? lsp)
      ; the client is not going to tell us when it has saved the file;
      ; we don't want to continuously evaluate text changes, so we should
      ; watch the disk for changes and use that as a signal to update
      (println "TODO: watch for changes")))

  {:capabilities
   {:codeActionProvider {:codeActionKinds [""
                                           :quickfix
                                           :source.organizeImports]}

    :completionProvider {:triggerCharacters ["/" "("]}

    :documentSymbolProvider true

    :textDocumentSync {:openClose true
                       :didSave true

                       ; 1 = sync the full file
                       ; 2 = incremental
                       :change 1}}

   :serverInfo {:name "sparkling"
                :version (util/version)}})

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
