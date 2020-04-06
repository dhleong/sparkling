(ns sparkling.nrepl.core
  (:require [nrepl.core :as nrepl]
            [sparkling.spec.util :refer [validate]]
            [sparkling.spec :as spec]))

(def message-timeout 1000)

(defn start [config]
  (validate ::spec/project-config config)

  ; TODO detect the port
  (nrepl/connect :port 63306))

(defn stop [conn]
  (.close conn))

(defn message [conn message]
  (-> (nrepl/client conn message-timeout)
      (nrepl/message message)
      nrepl/response-values))
