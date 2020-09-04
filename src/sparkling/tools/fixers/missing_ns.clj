(ns sparkling.tools.fixers.missing-ns
  (:require [promesa.core :as p]
            [sparkling.tools.edits.on-ns :as edits-on-ns]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(defn format-ns-fix [resolved]
  (let [candidates (:candidates resolved)
        definitive? (= (count candidates) 1)]
    ; TODO how to prompt for choices?
    (when definitive?
      {:description (str "Require " (first candidates) " :as " (:alias resolved))
       :target 'ns
       :namespace (first candidates)
       :alias (:alias resolved)
       :op edits-on-ns/insert-require})))

(def-fixer :missing-ns
  {:matches [#"No such namespace: ([^ ,]+)"
             #"Unresolved namespace ([^ .,]+)"]}
  [context sym]
  (p/let [candidates (resolve/missing-ns context sym)]
    (when candidates
      (format-ns-fix {:candidates candidates
                      :alias sym}))))
