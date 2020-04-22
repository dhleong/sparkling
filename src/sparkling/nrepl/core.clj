(ns sparkling.nrepl.core
  (:require [nrepl.core :as nrepl]
            [sparkling.nrepl.detect :as detect]
            [sparkling.path :as path]
            [sparkling.spec.util :refer [validate]]
            [sparkling.spec :as spec]))

(def message-timeout 1000)

(declare message)

(defn- try-shadow-connect [service]
  (let [{:keys [transport config]} @service
        build-id (->> (detect/shadow-builds config)
                     ; TODO configurable build-id filter
                     first)

        session (-> (nrepl/client transport message-timeout)
                    (nrepl/client-session))

        _ (println "attempt to connect to shadow build " build-id)

        resp (-> session
                 (nrepl/message {:op :eval
                                 :code (pr-str
                                         `(cider.piggieback/cljs-repl
                                            ~build-id))})
                 nrepl/combine-responses
                 :value
                 last
                 read-string)]

    (println "connected to shadow " build-id ": " resp)

    (when (= :no-worker (first resp))
      ; TODO should we auto-start?
      (throw (ex-info (str "Shadow worker for " build-id " not running")
                      {:build-id build-id
                       :notify? true})))

    (swap! service assoc :shadow/build build-id)

    session))

(defn try-cljs-connect [service]
  (let [{:keys [source]} @service]
    (case source
      :shadow (when-let [s (try-shadow-connect service)]
                (swap! service assoc :cljs s)
                s))))

(defn- connection-for [service msg]
  (let [{:keys [uri]} (:sparkling/context msg)
        {:keys [clj cljs]} @service]
    (or (when (= "cljs" (path/extension uri))
          (or cljs
              (try-cljs-connect service)))

        ; fallback to clj
        clj)))

(defn start [config]
  (validate ::spec/project-config config)

  (try
    (let [[source port] (detect/nrepl-port config)
          _ (println "Connecting to " source "@" port)
          transport (nrepl/connect :port port)]
      (atom {:clj (nrepl/client transport message-timeout)
             :transport transport
             :config config
             :source source}))
    (catch Throwable e
      (println e)
      (throw e))))

(defn stop [service]
  (.close (:transport @service)))

(defn message-seq [service msg]
  (println "message" (:op msg) (:ns msg) (:sparkling/context msg))
  (let [conn (connection-for service msg)
        msg (dissoc msg :sparkling/context)]
    (-> conn
        (nrepl/message msg))))

(defn message [service msg]
  (nrepl/combine-responses
    (message-seq service msg)))

