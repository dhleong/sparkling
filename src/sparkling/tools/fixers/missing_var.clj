(ns sparkling.tools.fixers.missing-var
  (:require [promesa.core :as p]
            [sparkling.tools.fixers.missing-ns :refer [format-ns-fix]]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(def-fixer :missing-var
  {:matches [#"No such var: ([^ ]+)$"
             #"unresolved symbol ([^ ]+)$"]}
  [context sym]
  (p/let [resolved (resolve/missing-var context sym)]
    (if resolved
      (case (:type resolved)
        :ns (format-ns-fix resolved))

      (println "Unable to resolve missing var: " sym)
      )
    ))
