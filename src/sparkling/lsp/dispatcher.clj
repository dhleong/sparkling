(ns sparkling.lsp.dispatcher
  (:require [promesa.core :as p]
            [sparkling.lsp.protocol :refer [errors]]))

(defn- method->kw [m]
  (when m
    (let [ns-separator (.indexOf m "/")]
      (if (= -1 ns-separator)
        (keyword m)
        (keyword (subs m 0 ns-separator)
                 (subs m (inc ns-separator)))))))

(defn- invoke-handler [request-id handler params]
  (-> (p/let [result (if (map? params)
                       (handler params)
                       (apply handler params))]
        {:id request-id
         :result result})

      (p/catch (fn [e]
                 {:id request-id
                  :error {:code (if-let [data (ex-data e)]
                                  (:error-code data)
                                  (:internal errors))
                          :message (or (ex-message e)
                                       "Unexpected error")}}))))

(defn create [handlers]
  (fn dispatch [request]
    (when-let [{:keys [id method params]} request]
      (if-let [handler (get handlers (method->kw method))]
        (invoke-handler id handler params)

        ; no handler
        (do
          (println "Reject unsupported method: " method)
          (p/rejected
            (ex-info (str method " is not supported")
                     {:request-id id
                      :error-code :method-not-found})))))))
