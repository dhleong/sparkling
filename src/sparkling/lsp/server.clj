(ns sparkling.lsp.server
  (:require [promesa.core :as p]
            [sparkling.lsp.protocol :refer [errors]]
            [sparkling.lsp.transport.model :refer [read-request write-message]]))

(defn- ->error [^Throwable e]
  (if-let [data (ex-data e)]
    {:id (:request-id e)
     :error {:code (:error-code data (:internal errors))
             :message (ex-message e)}}

    {:id 0
     :error {:code (:parse errors)
             :message (or (.getMessage e)
                          (str e))}}))

(defn start [& {:keys [transport dispatcher]}]
  (let [done (p/deferred)
        running-requests (atom {})]
    {:stop #(p/resolve! done ::done)

     :notify (fn [notification]
               (write-message transport notification))

     :cancel! (fn [request-id]
                (let [req (get @running-requests request-id)]
                  (swap! running-requests dissoc request-id)
                  (write-message transport {:id request-id
                                            :error {:code (:cancelled errors)
                                                    :message "Request cancelled"}})
                  (p/cancel! req)))

     :running running-requests

     :promise (p/loop []
                (p/let [next-packet (-> (p/race [(read-request transport)
                                                 done])
                                        (p/catch ->error))]

                  (cond
                    ; server was shutdown
                    (= ::done next-packet) nil

                    ; parse error:
                    (:error next-packet) (write-message transport next-packet)

                    ; normal case: defer handling so it can be cancelled
                    :else (swap! running-requests
                                 assoc
                                 (:id next-packet)
                                 (-> (dispatcher next-packet)
                                     (p/catch ->error)
                                     (p/then (fn [response]
                                               (swap! running-requests dissoc (:id next-packet))
                                               (write-message transport response))))))

                  (when-not (= ::done next-packet)
                    (p/recur))))}))
