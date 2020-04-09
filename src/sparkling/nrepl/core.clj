(ns sparkling.nrepl.core
  (:require [nrepl.core :as nrepl]
            [sparkling.nrepl.detect :refer [detect-port]]
            [sparkling.path :as path]
            [sparkling.spec.util :refer [validate]]
            [sparkling.spec :as spec]))

(def message-timeout 1000)

(defn- connection-for [service msg]
  (let [{:keys [uri]} (:sparkling/context msg)]
    (or (when (= "cljs" (path/extension uri))
          (:cljs service))
        (:clj service))))

(defn start [config]
  (validate ::spec/project-config config)

  (try
    (let [[source port] (detect-port config)]
      (println "Connecting to " source "@" port)
      {:clj (nrepl/connect :port port)
       :source source})
    (catch Throwable e
      (println e)
      (throw e))))

(defn stop [service]
  (.close (:clj service))
  (when-let [cljs (:cljs service)]
    (.close cljs)))

(defn message [service msg]
  (let [conn (connection-for service msg)
        msg (dissoc msg :sparkling/context)]
    (-> (nrepl/client conn message-timeout)
        (nrepl/message msg)
        nrepl/combine-responses)))
