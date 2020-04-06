(ns sparkling.core
  (:require [systemic.core :as systemic :refer [defsys]]
            [sparkling.handlers :as handlers]
            [sparkling.lsp :as lsp-util]
            [sparkling.lsp.core :as lsp]))

; ======= system defs =====================================

(defsys *handlers*
  :start (handlers/get-all))

(defsys *lsp*
  :deps [*handlers*]
  :closure
  (let [{:keys [stop] :as value} (lsp/start *handlers*)]
    {:value value
     :stop stop}))


; ======= main ============================================

(defn- run-lsp []
  ; redirect stdout to stderr for debugging
  (alter-var-root #'*out* (constantly *err*))

  (systemic/start!)

  ; wait forever
  @(:promise (lsp-util/instance)))

(defn -main [& args]
  (cond
    (= ["lsp" "--stdio"] args) (run-lsp)

    :else (do
            (println "sparkling: invalid")
            (System/exit 1))))
