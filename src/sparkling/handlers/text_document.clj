(ns sparkling.handlers.text-document
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.handlers.text-sync :as text-sync :refer [*doc-state*]]
            [sparkling.lsp.fix :as lsp-fix]
            [sparkling.lsp.protocol :as protocol]
            [sparkling.path :as path]
            [sparkling.spec.util :refer [validate]]
            [sparkling.tools.autofix :refer [find-diagnostics]]
            [sparkling.tools.completion :refer [suggest-complete]]
            [sparkling.tools.definition :refer [find-definition]]
            [sparkling.tools.references :refer [find-references]]
            [sparkling.tools.edit :as edit]
            [sparkling.tools.fix :as fix]
            [sparkling.tools.highlight :as highlight]))


; ======= completion ======================================

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


; ======= codeAction ======================================

(defn- fix->lsp-edit [{:keys [diagnostics] :as context} fix]
  (p/let [fix fix
          _ (println "found fix: " fix)

          ; insert the current doc :text so we
          ; can extract an edit
          edit (when fix
                 (edit/fix->edit
                   (assoc fix :text (:document-text context))))

          ; convert into lsp format
          lsp-edit (when edit
                     (lsp-fix/->text-edit context edit))]
    (when lsp-edit
      {:title (:title lsp-edit)
       :diagnostics diagnostics
       :kind :quickfix  ; anything else ?
       :edit (validate
               ::protocol/workspace-edit
               (dissoc lsp-edit :title))})))

(defn- create-fix-edit [context diagnostic]
  (println "Attempt to fix" (pr-str diagnostic) "...")
  (some->>
    (fix/extract context (:message diagnostic))
    (fix->lsp-edit (assoc context :diagnostics [diagnostic]))))

(defhandler :textDocument/codeAction [{{:keys [diagnostics]} :context,
                                       {uri :uri} :textDocument
                                       {{:keys [line]} :start} :range
                                       :as req}]
  (println "Code action for" uri ": " diagnostics)
  (println " req = " req)

  (p/let [config *project-config*
          context {:uri uri
                   :document-text (doc-state-of uri)
                   :root-path (:root-path config)
                   :line line}
          fixes (if (seq diagnostics)
                  ; we provided diagnostics that the client wants to fix
                  (->> diagnostics
                       (keep (partial create-fix-edit context))
                       (p/all))

                  ; no known diagnostics... fix lint error maybe?
                  (p/let [fixes (find-diagnostics context)]
                    (println "fixes=" fixes)
                    (->> fixes
                         (keep (partial create-fix-edit context))
                         (p/all))))

          ; TODO ask client to re-check for errors after fixing
          ;; _ (text-sync/check-for-errors uri nil)
          ]

    (prn "edits=" fixes)
    (keep identity fixes)))


; ======= definition ======================================

(defhandler :textDocument/definition [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (doc-state-of uri)
          locations (find-definition {:document-text doc
                                      :document-ns (path/->ns uri)
                                      :uri uri
                                      :character character
                                      :line line
                                      :sparkling/context {:uri uri}})]

    (->> locations
         (map (fn [{:keys [uri col line]}]
                (let [start {:line line :character col}]
                  {:uri uri
                   :range {:start start
                           :end start}}))))))


; ======= documentSymbol ==================================

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


; ======= references ======================================

(defhandler :textDocument/references [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (doc-state-of uri)
          locations (find-references {:document-text doc
                                      :document-ns (path/->ns uri)
                                      :uri uri
                                      :character character
                                      :line line
                                      :sparkling/context {:uri uri}})]

    (->> locations
         (map (fn [{:keys [uri col line]}]
                (let [start {:line line :character col}]
                  {:uri uri
                   :range {:start start
                           :end start}}))))))
