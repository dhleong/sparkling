(ns sparkling.core
  (:require [systemic.core :as systemic :refer [defsys]]
            [sparkling.handlers :as handlers]
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

(defn- lsp-instance []
  (let [lsp *lsp*]
    ; NOTE: the docs for systemic say its value should be what
    ;  was returned in :value, but that does not seem to be the case
    (if (:promise lsp)
      lsp
      (:value lsp))))

; ======= public api ======================================

(defn cancel-request! [request-id]
  ((:cancel! (lsp-instance)) request-id))

(defn notify! [message]
  (if-let [send! (:notify (lsp-instance))]
    (send! message)
    (println "WARN: *lsp* not started yet")))


; ======= main ============================================

(defn -main [& _args]
  (systemic/start!)

  ; wait forever
  @(:promise (lsp-instance)))
