(ns sparkling.nrepl
  (:require [nrepl.core :as nrepl]
            [promesa.core :as p]
            [promesa.exec :as exec]
            [systemic.core :refer [defsys]]
            [sparkling.config :refer [*project-config*]]
            [sparkling.nrepl.core :as core]))

(defsys *nrepl*
  "A Promise that resolves to the nrepl connection info map."
  :deps [*project-config*]
  :closure
  (let [server (-> *project-config*
                   (p/then core/start))]
    {:value server
     :stop #(when (p/resolved? server)
              (core/stop @server))}))


; ======= public interface ================================

(defn message
  "Send the given message to the nrepl server, returning a promise that
   resolves to the response. If *nrepl* has not yet been initialized
   (usually due to *project-config* not yet being resolved) this promise
   will wait for *nrepl* to initialize before sending."
  [msg]
  (-> *nrepl*
      (p/then (fn [server]
                (core/message (:conn server) msg))
              exec/default-scheduler)
      (p/then (fn [resp]
                (if-let [err (:err resp)]
                  (throw (ex-info err
                                  (assoc resp :message msg)))

                  resp)))))

(defn evaluate* [code-str]
  (-> (message {:op :eval
                :code code-str})
      (p/then (fn [resp]
                (->> resp :value last read-string)))))

(defmacro evaluate [& code]
  (let [the-code `(nrepl/code ~@code)]
    `(evaluate* ~the-code)))
