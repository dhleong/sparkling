(ns sparkling.tools.fixers.missing-var
  (:require [promesa.core :as p]
            [sparkling.tools.edits.on-ns :as edits-on-ns]
            [sparkling.tools.fixers.missing-ns :refer [format-ns-fix]]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(defn format-refer-fix [resolved]
  (let [candidates (:candidates resolved)]
    (->> candidates
         (map (fn [{:keys [candidate ns]}]
                {:description (str "Require " ns " :refer " candidate)
                 :target 'ns
                 :namespace ns
                 :symbol candidate
                 :op edits-on-ns/insert-refer})))))

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
