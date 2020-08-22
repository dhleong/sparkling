(ns sparkling.handlers.text-document
  (:require [promesa.core :as p]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.handlers.text-sync :as text-sync :refer [*doc-state*]]
            [sparkling.lsp.fix :as lsp-fix]
            [sparkling.lsp.protocol :as protocol]
            [sparkling.path :as path]
            [sparkling.spec.util :refer [validate]]
            [sparkling.tools.completion :refer [suggest-complete]]
            [sparkling.tools.edit :as edit]
            [sparkling.tools.fix :as fix]
            [sparkling.tools.highlight :as highlight]))

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

(def var-type->completion-kind
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

(defn doc-state-of [uri]
  (or (get @*doc-state* uri)
      (throw (IllegalArgumentException.
               (str "Not opened: " uri)))))

(defhandler :textDocument/completion [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (doc-state-of uri)
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
                         :kind (var-type->completion-kind (keyword (:type c)))

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
                     (keep (fn [d]
                             (println "Attempt to fix" (pr-str d) "...")
                             (some->
                               (fix/extract context (:message d))
                               (p/chain
                                 ; insert the current doc :text so we
                                 ; can extract an edit
                                 (fn [fix]
                                   (println "found fix: " fix)
                                   (edit/fix->edit
                                     (assoc fix :text (doc-state-of uri))))

                                 (partial lsp-fix/->text-edit context)

                                 (fn [text-edit]
                                   {:title (:title text-edit)
                                    :diagnostics [d]
                                    :edit (validate
                                            ::protocol/workspace-edit
                                            (dissoc text-edit :title))})))))
                     (p/all))

          ; TODO ask client to re-check for errors after fixing
          ;; _ (text-sync/check-for-errors uri nil)
          ]

    (prn "fixes=" fixes)
    (keep identity fixes)))

(def ^:private lsp-symbol-kind-namespace 3)
(def ^:private lsp-symbol-kind-class 5)
(def ^:private lsp-symbol-kind-method 6)
(def ^:private lsp-symbol-kind-function 12)
(def ^:private lsp-symbol-kind-variable 13)

(def ^:private highlight-kind->symbol-kind
  {:fn lsp-symbol-kind-function
   :macro lsp-symbol-kind-method
   :ns lsp-symbol-kind-namespace
   :class lsp-symbol-kind-class
   :var lsp-symbol-kind-variable})

(defhandler :textDocument/documentSymbol [{{uri :uri} :textDocument}]
  (println "documentSymbol")
  (p/let [highlights (highlight/types-in uri (doc-state-of uri))]
    (->> highlights
         (mapcat (fn [[kind symbol-names]]
                   (let [symbol-kind (highlight-kind->symbol-kind kind)]
                     (->> symbol-names
                          (map (fn [symbol-name]
                                 ; TODO :location ?
                                 {:name symbol-name
                                  :kind symbol-kind})))))))))
