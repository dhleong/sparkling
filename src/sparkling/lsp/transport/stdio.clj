(ns sparkling.lsp.transport.stdio
  "An stdio transport"
  (:require [sparkling.lsp.transport.okio :as okio])
  (:import (okio Okio)))

(defn create [{:keys [_parse-in _format-out] :as opts}]
  (okio/create
    (assoc opts
           :in (Okio/buffer (Okio/source System/in))
           :out (Okio/buffer (Okio/sink System/out)))))

