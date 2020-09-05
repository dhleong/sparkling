(ns sparkling.tools.fixers.duplicate-refer
  (:require [sparkling.nrepl :as nrepl]
            [sparkling.tools.fixers.model :refer [def-fixer]]))

(def-fixer :duplicate-refer
  {:matches [#"^(.+) already refers to"]}
  [_context sym]
  ; NOTE: this depends on refactor-nrepl
  (println "undef" sym)
  {:description (str "Undef " sym)
   :op (fn undef-duplicate-refer [_]
         (println "perform undef" sym)
         (nrepl/message {:op :undef
                         :symbol sym}))})
