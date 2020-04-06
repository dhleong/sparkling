(ns sparkling.nrepl
  (:require [nrepl.core :as nrepl]
            [promesa.core :as p]
            [systemic.core :refer [defsys]]
            [sparkling.config :refer [*project-config*]]
            [sparkling.nrepl.core :as core]))

(defsys *nrepl*
  "This is a Promise that resolves to the nrepl connection,
   since project config is also a Promsie"
  :deps [*project-config*]
  :closure
  (let [server (-> *project-config*
                   (p/then core/start))]
    {:value server
     :stop #(when (p/resolved? server)
              (core/stop server))}))


; ======= public interface ================================

(defn message [message]
  (core/message *nrepl* message))

(defmacro evaluate [& code]
  (let [the-code `(nrepl/code ~@code)]
    `(message {:op "eval"
               :code  ~the-code})))
