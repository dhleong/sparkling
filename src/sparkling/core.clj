(ns sparkling.core
  (:require [systemic.core :as systemic]
            [sparkling.config]
            [sparkling.handlers]
            [sparkling.lsp :refer [*lsp*]]
            [sparkling.nrepl]))

(defn- run-lsp []
  ; redirect stdout to stderr for debugging
  (alter-var-root #'*out* (constantly *err*))

  (systemic/start!)

  ; wait forever
  @(:promise *lsp*))


; ======= main ============================================

(defn -main [& args]
  (cond
    (= ["lsp" "--stdio"] args) (run-lsp)

    :else (do
            (println "sparkling: invalid")
            (System/exit 1))))
