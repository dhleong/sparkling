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

(defn- clean-message [message]
  (update message :method (fn [m]
                            (if (string? m)
                              m
                              (str (namespace m)
                                   "/"
                                   (name m))))))

; ======= public api ======================================

(defn cancel-request! [request-id]
  ((:cancel! (instance)) request-id))

(defn notify!
  ([method params] (notify! {:method method
                             :params params}))
  ([message]
   (if-let [send! (:notify (instance))]
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
