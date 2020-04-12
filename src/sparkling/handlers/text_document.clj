(ns sparkling.handlers.text-document
  (:require [promesa.core :as p]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.handlers.text-sync :as text-sync :refer [*doc-state*]]
            [sparkling.lsp.fix :as lsp-fix]
            [sparkling.lsp.protocol :as protocol]
            [sparkling.path :as path]
            [sparkling.spec.util :refer [validate]]
            [sparkling.tools.completion :refer [suggest-complete]]
            [sparkling.tools.fix :as fix]))

(def identifier-chars "([a-zA-Z._*:$!?+=<>$/-]+)")
(def regex-identifier-tail (re-pattern (str identifier-chars "$")))
(def regex-identifier-head (re-pattern (str "^" identifier-chars)))

(def ^:private lsp-completion-kind-method 2)
(def ^:private lsp-completion-kind-function 3)
;; (def ^:private lsp-completion-kind-constructor 4)
(def ^:private lsp-completion-kind-field 5)
(def ^:private lsp-completion-kind-variable 6)
(def ^:private lsp-completion-kind-class 7)
;; (def ^:private lsp-completion-kind-interface 8)
(def ^:private lsp-completion-kind-module 9)
;; (def ^:private lsp-completion-kind-property 10)
;; (def ^:private lsp-completion-kind-unit 11)
;; (def ^:private lsp-completion-kind-value 12)
(def ^:private lsp-completion-kind-enum 13)
(def ^:private lsp-completion-kind-keyword 14)
;; (def ^:private lsp-completion-kind-snippet 15)
;; (def ^:private lsp-completion-kind-color 16)
(def ^:private lsp-completion-kind-file 17)
;; (def ^:private lsp-completion-kind-reference 18)
;; (def ^:private lsp-completion-kind-folder 19)
;; (def ^:private lsp-completion-kind-enumMember 20)
;; (def ^:private lsp-completion-kind-constant 21)
;; (def ^:private lsp-completion-kind-struct 22)
;; (def ^:private lsp-completion-kind-event 23)
;; (def ^:private lsp-completion-kind-operator 24)
;; (def ^:private lsp-completion-kind-typeParameter 25)

(def var-type->kind
  {:function lsp-completion-kind-function
   :macro lsp-completion-kind-method
   :var lsp-completion-kind-variable
   :special-form lsp-completion-kind-enum
   :class lsp-completion-kind-class
   :keyword lsp-completion-kind-keyword
   :local lsp-completion-kind-variable
   :namespace lsp-completion-kind-module
   :field lsp-completion-kind-field
   :method lsp-completion-kind-method
   :static-field lsp-completion-kind-field
   :static-method lsp-completion-kind-method
   :resource lsp-completion-kind-file})

(defhandler :textDocument/completion [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (or (get @*doc-state* uri)
                  (throw (IllegalArgumentException.
                           (str "Not opened: " uri))))
          completions (suggest-complete {:document-text doc
                                         :document-ns (path/->ns uri)
                                         :character character
                                         :line line
                                         :sparkling/context {:uri uri}})]

    ; TODO show ns somewhere?
    {:isIncomplete true
     :items (->> completions
                 (map (fn [c]
                        {:label (:candidate c)
                         :kind (var-type->kind (keyword (:type c)))

                         :detail (when-let [args (seq (:arglists c))]
                                   (str args))
                         :documentation (:doc c)})))}))

(defhandler :textDocument/codeAction [{{:keys [diagnostics]} :context,
                                       {uri :uri} :textDocument
                                       :as req}]
  (println "Code action for" uri ": " diagnostics)
  (println " req = " req)

  (p/let [context {:uri uri}
          fixes (->> diagnostics
                     (map (fn [d]
                            (println "Attempt to fix" (pr-str d) "...")
                            (-> (fix/apply-fix context (:message d))
                                (p/then' (partial lsp-fix/->text-edit context))
                                (p/then' (fn [edit]
                                           {:title (str "Fix: " (:message d))
                                            :edit (validate
                                                    ::protocol/workspace-edit
                                                    edit)})))))
                     (p/all))

          ; re-check for errors after fixing
          ; (TODO actually, we probably need to ask the client to do
          ; this after they apply edits
          _ (text-sync/check-for-errors uri nil)
          ]

    (println "TODO: fixes=" fixes)

    (keep identity fixes)))
