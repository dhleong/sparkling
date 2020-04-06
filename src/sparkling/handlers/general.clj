(ns sparkling.handlers.general
  (:require [sparkling.handlers.core :refer [defhandler]]))

(defhandler :initialize [{:keys [_clientInfo capabilities _rootPath]}]
  (println ":initialize" capabilities)
  {:capabilities
   {:textDocumentSync {:openClose false

                       ; 1 = sync the full file
                       ; 2 = incremental
                       :change 1}}

   :serverInfo {:name "sparkling"
                :version "SNAPSHOT"}})

(defhandler :initialized [params]
  (println "Client initialized!" params))
