(ns sparkling.handlers
  (:require [sparkling.handlers.core :refer [handlers]]

            ; require namespaces to define handlers:
            [sparkling.handlers.base]
            [sparkling.handlers.general]
            [sparkling.handlers.text-sync]
            [sparkling.handlers.workspace]))

(defn get-all []
  @handlers)

