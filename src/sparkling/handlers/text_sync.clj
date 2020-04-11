(ns sparkling.handlers.text-sync
  (:require [promesa.core :as p]
            [sparkling.tools.analyze :as analyze]
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

          (lsp/notify!
            :textDocument/publishDiagnostics
            {:uri uri
             :version version
             :diagnostics (if diagnostic
                            [(parse-diagnostic diagnostic)]
                            [])}))

        (p/catch (fn [e]
                   (println "Unexpected error" e)
                   (when-let [msg (ex-message e)]
                     (lsp/log! :warn msg)))))))


; ======= handlers ========================================

(defhandler :textDocument/didOpen [{{:keys [uri languageId text]} :textDocument}]
  (println "Opened: " languageId "@" uri)
  (when text
    (swap! *doc-state* assoc uri text))
  (check-for-errors uri 0))

(defhandler :textDocument/didClose [{{:keys [uri]} :textDocument}]
  (println "Closed " uri)
  (swap! *doc-state* dissoc uri))

(defhandler :textDocument/didChange [{{:keys [uri version]} :textDocument, changes :contentChanges}]
  (println "Change: " uri "@" version ": " (count changes))
  (apply-changes! uri changes)

  (check-for-errors uri version))
