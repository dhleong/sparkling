(ns sparkling.handlers
  (:require [sparkling.handlers.core :refer [handlers]]

            ; require namespaces to define handlers
            [sparkling.handlers.general]
            ))

(defn get-all []
  @handlers)
