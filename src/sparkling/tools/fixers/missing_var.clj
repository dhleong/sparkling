(ns sparkling.tools.fixers.missing-var
  (:require [promesa.core :as p]
            [sparkling.tools.edits.on-ns :as edits-on-ns]
            [sparkling.tools.fixers.missing-ns :refer [format-ns-fix]]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(defn format-refer-fix [resolved]
  (let [candidates (:candidates resolved)
        definitive? (= (count candidates) 1)]
    ; TODO how to prompt for choices?
    (when definitive?
      (let [c (first candidates)]
        ; FIXME: namespace, symbol, etc.
        {:description (str "Require " c " :refer " c)
         :target 'ns
         :namespace c
         :symbol c
         :op edits-on-ns/insert-refer}))))

(def-fixer :missing-var
  {:matches [#"No such var: ([^ ]+)$"
             #"unresolved symbol ([^ ]+)$"]}
  [context sym]
  (p/let [resolved (resolve/missing-var context sym)]
    (if resolved
      (case (:type resolved)
        :ns (format-ns-fix resolved)
        :symbol (format-refer-fix resolved))

      (println "Unable to resolve missing var: " sym)
      )
    ))
