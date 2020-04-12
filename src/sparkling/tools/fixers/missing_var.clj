(ns sparkling.tools.fixers.missing-var
  (:require [promesa.core :as p]
            [sparkling.tools.edits.on-ns :as edits-on-ns]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(defn format-ns-fix [resolved]
  (let [candidates (:candidates resolved)
        definitive? (= (count candidates) 1)]
    ; TODO how to prompt for choices?
    (when definitive?
      {:target 'ns
       :ns (first candidates)
       :alias (:alias resolved)
       :op edits-on-ns/insert-require})))

(def-fixer :missing-var
  {:matches [#"No such var: ([^ ]+)$"]}
  [context sym]
  (p/let [resolved (resolve/missing-var context sym)]
    (when resolved
      (println "Fix " resolved)
      (case (:type resolved)
        :ns (format-ns-fix resolved)))
    ))
