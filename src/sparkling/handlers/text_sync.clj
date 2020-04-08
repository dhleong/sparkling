(ns sparkling.handlers.text-sync
  (:require [promesa.core :as p]
            [sparkling.analyze :as analyze]
            [sparkling.lsp :as lsp]
            [systemic.core :refer [defsys]]
            [sparkling.config :as config]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.nrepl :as nrepl :refer [*nrepl*]]
            [sparkling.nrepl.lsp :refer [parse-diagnostic]]))

(defsys *doc-state*
  :deps [*nrepl*]
  :start (atom {}))

(defn- apply-changes! [uri changes]
  ; TODO incremental?
  (swap! *doc-state* assoc uri (-> changes first :text)))

(defn- check-for-errors [uri version]
  (when-not (config/lsp :did-save?)
    (-> (p/let [code (get @*doc-state* uri)
                diagnostic (when code
                             (analyze/string uri code))]

          (if diagnostic
            ; error discovered
            (lsp/notify!
              :textDocument/publishDiagnostics
              {:uri uri
               :version version
               :diagnostics
               [(parse-diagnostic diagnostic)]})

            (lsp/notify!
              :textDocument/publishDiagnostics
              {:uri uri
               :version version
               :diagnostics []})))

        (p/catch (fn [e]
                   (println "Unexpected error" e)
                   (lsp/notify!
                     :textDocument/publishDiagnostics
                     {:uri uri
                      :version version
                      :diagnostics
                      [(parse-diagnostic e)]}))))))


; ======= handlers ========================================

(defhandler :textDocument/didOpen [{{:keys [uri languageId text]} :textDocument}]
  (println "Opened: " languageId "@" uri)
  (when text
    (swap! *doc-state* assoc uri text))
  (check-for-errors uri 0))

(defhandler :textDocument/didClose [{{:keys [uri]} :textDocument}]
  (println "Closed " uri))

(defhandler :textDocument/didChange [{{:keys [uri version]} :textDocument, changes :contentChanges}]
  (println "Change: " uri "@" version ": " (count changes))
  (apply-changes! uri changes)

  (check-for-errors uri version))
