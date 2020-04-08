(ns sparkling.lsp.transport.model
  (:require [clojure.string :as str]))

(defprotocol ITransport
  (read-request [this])
  (write-message [this message]))
