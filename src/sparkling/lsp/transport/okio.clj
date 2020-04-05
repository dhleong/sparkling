(ns sparkling.lsp.transport.okio
  "An okio-based transport"
  (:require [promesa.core :as p]
            [sparkling.lsp.transport.model :refer [ITransport]])
  (:import (java.util.concurrent Executors)))

(defonce ^:private executor-in (Executors/newSingleThreadExecutor))
(defonce ^:private executor-out (Executors/newSingleThreadExecutor))

(deftype StreamTransport [in out parse-in format-out]
  ITransport
  (read-request [this]
    (p/create
      (fn [p-resolve p-reject]
        (try
          (p-resolve
            (parse-in in))
          (catch Exception e
            (p-reject e))))
      executor-in))

  (write-message [this message]
    (p/create
      (fn [p-resolve p-reject]
        (try
          (format-out out message)
          (p-resolve message)
          (catch Exception e
            (p-reject e))))
      executor-out)))

(defn create
  [{:keys [in out parse-in format-out]}]
  (->StreamTransport in out parse-in format-out))
