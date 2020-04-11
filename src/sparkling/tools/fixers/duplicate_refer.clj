(ns sparkling.tools.fixers.duplicate-refer
  (:require [sparkling.nrepl :as nrepl]
            [sparkling.tools.fixers.model :refer [def-fixer]]))

(def-fixer :duplicate-refer
  {:matches [#"^(.+) already refers to"]}
  [sym]
  ; NOTE: this depends on refactor-nrepl
  (println "undef" sym)
  (nrepl/message {:op :undef
                  :symbol sym}))
