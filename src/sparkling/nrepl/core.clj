(ns sparkling.nrepl.core
  (:require [nrepl.core :as nrepl]
            [sparkling.nrepl.detect :refer [detect-port]]
            [sparkling.spec.util :refer [validate]]
            [sparkling.spec :as spec]))

(def message-timeout 1000)

(defn start [config]
  (validate ::spec/project-config config)

  (try
    (let [[source port] (detect-port config)]
      (println "Connecting to " source "@" port)
      {:conn (nrepl/connect :port port)
       :source source})
    (catch Throwable e
      (println e)
      (throw e))))

(defn stop [conn]
  (.close (:conn conn)))

(defn message [conn msg]
  (-> (nrepl/client conn message-timeout)
      (nrepl/message msg)
      nrepl/combine-responses))
