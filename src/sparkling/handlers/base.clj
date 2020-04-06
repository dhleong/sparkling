(ns sparkling.handlers.base
  (:require [sparkling.core :refer [cancel-request!]]
            [sparkling.handlers.core :refer [defhandler]]))

(defhandler :$/cancelRequest [{:keys [id]}]
  (cancel-request! id))
