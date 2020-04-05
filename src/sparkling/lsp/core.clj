(ns sparkling.lsp.core
  (:require [sparkling.lsp.dispatcher :as dispatcher]
            [sparkling.lsp.server :as server]
            [sparkling.lsp.wire :as wire]
            [sparkling.lsp.transport.stdio :as stdio]))

(defn start [handlers]
  (server/start
    :dispatcher (dispatcher/create handlers)
    :transport (stdio/create
                 {:parse-in wire/parse-in
                  :format-out wire/format-out})))
