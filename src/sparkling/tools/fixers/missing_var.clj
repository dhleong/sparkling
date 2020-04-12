(ns sparkling.tools.fixers.missing-var
  (:require [promesa.core :as p]
            [sparkling.tools.fixers.missing-ns :refer [format-ns-fix]]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(def-fixer :missing-var
  {:matches [#"No such var: ([^ ]+)$"]}
  [context sym]
  (p/let [resolved (resolve/missing-var context sym)]
    (when resolved
      (println "Fix " resolved)
      (case (:type resolved)
        :ns (format-ns-fix resolved)))
    ))
