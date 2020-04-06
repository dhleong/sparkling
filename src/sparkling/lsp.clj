(ns sparkling.lsp
  (:require [systemic.core :refer [defsys]]
            [sparkling.lsp.core :as core]))

(defn- clean-message [message]
  (update message :method (fn [m]
                            (if (string? m)
                              m
                              (str (namespace m)
                                   "/"
                                   (name m))))))

; ======= public api ======================================

(defsys *handlers*
  ; NOTE: since handlers might want to call notify! etc,
  ; we have to dynamically resolve the get-all var to
  ; avoid a circular dependency
  :start (@(resolve 'sparkling.handlers/get-all)))

(defsys *lsp*
  :deps [*handlers*]
  :closure
  (let [{:keys [stop] :as value} (core/start *handlers*)]
    {:value value
     :stop stop}))

(defn cancel-request! [request-id]
  ((:cancel! *lsp*) request-id))

(defn notify!
  ([method params] (notify! {:method method
                             :params params}))
  ([message]
   (if-let [send! (:notify *lsp*)]
     (send! (clean-message message))
     (println "WARN: *lsp* not started yet"))))

(defn log!
  ([message] (log! :log message))
  ([msg-type message]
   (notify! :window/showMessage
            {:type (case msg-type
                     :error 1
                     :warn 2
                     :info 3
                     4)
             :message message})))
