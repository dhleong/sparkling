(ns sparkling.handlers.text-sync
  (:require [sparkling.handlers.core :refer [defhandler]]))

(defhandler :textDocument/didOpen [{{:keys [uri languageId _text]} :textDocument}]
  (println "Opened: " languageId "@" uri))
