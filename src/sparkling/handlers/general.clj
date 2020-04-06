(ns sparkling.handlers.general
  (:require [sparkling.handlers.core :refer [defhandler]]))

(defhandler :initialize [{:keys [clientInfo capabilities rootPath]}]
  {:capabilities {}
   :serverInfo {:name "sparkling"
                :version "SNAPSHOT"}})
