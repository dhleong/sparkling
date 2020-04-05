(ns sparkling.handlers.general
  (:require [sparkling.handlers.core :refer [defhandler]]))

(defhandler :initialize []
  (println "INITIALIZED"))
