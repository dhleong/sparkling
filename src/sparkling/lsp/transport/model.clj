(ns sparkling.lsp.transport.model)

(defprotocol ITransport
  (read-request [this])
  (write-message [this message]))
