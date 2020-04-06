(ns sparkling.handlers.base
  (:require [sparkling.lsp :as lsp]
            [sparkling.handlers.core :refer [defhandler]]))

(defhandler :$/cancelRequest [{:keys [id]}]
  (lsp/cancel-request! id))
