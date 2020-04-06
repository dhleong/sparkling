(ns sparkling.lsp)

(def ^:private get-*lsp*
  (delay
    (resolve 'sparkling.core/*lsp*)))

(defn instance []
  (let [lsp @@get-*lsp*]
    ; NOTE: the docs for systemic say its value should be what
    ;  was returned in :value, but that does not seem to be the case
    (if (:promise lsp)
      lsp
      (:value lsp))))


; ======= public api ======================================

(defn cancel-request! [request-id]
  ((:cancel! (instance)) request-id))

(defn notify! [message]
  (if-let [send! (:notify (instance))]
    (send! message)
    (println "WARN: *lsp* not started yet")))


