(ns sparkling.core
  (:require [promesa.core :as p]
            [systemic.core :as systemic :refer [defsys]]
            [sparkling.handlers :as handlers]
            [sparkling.lsp.core :as lsp]
            [sparkling.nrepl.core :as nrepl]))

; ======= system defs =====================================

(defsys *handlers*
  :start (handlers/get-all))

(defsys *lsp*
  :deps [*handlers*]
  :closure
  (let [{:keys [stop] :as value} (lsp/start *handlers*)]
    {:value value
     :stop stop}))

(defsys *project-config*
  "This is a Promise that will be resolved to a map matching
   ::spec/project-config"
  :start (p/deferred))

(defsys *nrepl*
  "This is a Promise that resolves to the nrepl connection,
   since project config is also a Promsie"
  :deps [*project-config*]
  :closure
  (let [server (-> *project-config*
                   (p/then nrepl/start))]
    {:value server
     :stop #(when (p/resolved? server)
              (nrepl/stop server))}))


; ======= main ============================================

(defn- run-lsp []
  ; redirect stdout to stderr for debugging
  (alter-var-root #'*out* (constantly *err*))

  (systemic/start!)

  ; wait forever
  @(:promise *lsp*))

(defn -main [& args]
  (cond
    (= ["lsp" "--stdio"] args) (run-lsp)

    :else (do
            (println "sparkling: invalid")
            (System/exit 1))))
