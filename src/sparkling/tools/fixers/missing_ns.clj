(ns sparkling.tools.fixers.missing-ns
  (:require [promesa.core :as p]
            [sparkling.tools.edits.on-ns :as edits-on-ns]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(defn format-ns-fix [resolved]
  (->> resolved
       :candidates
       (map (fn [candidate]
              {:description (str "Require " candidate " :as " (:alias resolved))
               :target 'ns
               :namespace candidate
               :alias (:alias resolved)
               :op edits-on-ns/insert-require}))))

(def-fixer :missing-ns
  {:matches [#"No such namespace: ([^ ,]+)"
             #"Unresolved namespace ([^ .,]+)"]}
  [context sym]
  (p/let [candidates (resolve/missing-ns context sym)]
    (when candidates
      (format-ns-fix {:candidates candidates
                      :alias sym}))))
