(ns sparkling.lsp.server
  (:require [promesa.core :as p]
            [sparkling.lsp.protocol :refer [errors]]
            [sparkling.lsp.transport.model :refer [read-request write-message]]))

(defn- ->parse-error [^Throwable e]
  {:id 0
   :error {:code (:parse errors)
           :message (or (.getMessage e)
                        (str e))}})

(defn start [& {:keys [transport dispatcher]}]
  (let [done (p/deferred)]
    {:stop #(p/resolve! done ::done)

     :notify (fn [notification]
               (write-message transport notification))

     :promise (p/loop []
                (p/let [next-packet (p/race [done
                                             (-> (read-request transport)
                                                 (p/catch ->parse-error))])
                        response (cond
                                   (= ::done next-packet) nil

                                   ; parse error:
                                   (:error next-packet) next-packet

                                   :else (dispatcher next-packet))]

                  (when response
                    (write-message transport response))

                  (when-not (= ::done next-packet)
                    (p/recur))))}))
