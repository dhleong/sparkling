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

(defn create [handlers]
  (fn dispatch [request]
    (when-let [{:keys [id method params]} request]
      (when-let [handler (get handlers (method->kw method))]
        (-> (p/let [result (if (map? params)
                               (handler params)
                               (apply handler params))]
              {:id id
               :result result})

            (p/catch (fn [e]
                       ; TODO more descriptive error codes?
                       {:id id
                        :error {:code (:internal errors)
                                :message (or (ex-message e)
                                             "Unexpected error")}})))))))
