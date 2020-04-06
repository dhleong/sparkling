(ns sparkling.nrepl
  (:require [sparkling.nrepl.core :as core]
            [nrepl.core :as nrepl]))

(def ^:private get-*nrepl*
  (delay
    (resolve 'sparkling.core/*nrepl*)))

(defn- instance []
  @@get-*nrepl*)


; ======= public interface ================================

(defn message [message]
  (core/message (instance) message))

(defmacro evaluate [& code]
  (let [the-code `(nrepl/code ~@code)]
    `(message {:op "eval"
               :code  ~the-code})))
