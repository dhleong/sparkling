(ns sparkling.handlers.workspace
  (:require [sparkling.handlers.core :refer [defhandler]]))

(defhandler :workspace/didChangeConfiguration [{:keys [settings]}]
  (println "DidChangeConfiguration: " settings))
