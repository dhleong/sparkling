(ns sparkling.nrepl
  (:require [clojure.set :refer [map-invert]]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [nrepl.core :as nrepl]
            [promesa.core :as p]
            [promesa.exec :as exec]
            [systemic.core :as systemic :refer [defsys]]
            [sparkling.config :refer [*project-config*]]
            [sparkling.nrepl.core :as core]))

(defsys *nrepl*
  "A Promise that resolves to the nrepl connection info map."
  :deps [*project-config*]
  :closure
  (let [server (-> *project-config*
                   (p/then core/start))]
    {:value server
     :stop #(when (p/resolved? server)
              (core/stop @server))}))

(defn- parse-message-response [msg resp]
  (if-let [err (:err resp)]
    (throw (ex-info err
                    (assoc resp :message msg)))

    resp))

(defn- ensure-ns-loaded [context server ns-sym]
  (let [msg (merge context
                   {:op :eval
                    :code (str "(ns " (:user-ns @server "user")
                               " (:require [" ns-sym "]))")})]
    (-> server
        (core/message msg)
        (parse-message-response msg))))

(defn- message* [f msg]
  (when-not (systemic/running? `*nrepl*)
    (throw (IllegalStateException. "Not connected to nrepl")))

  (-> *nrepl*
      (p/then (fn [server]
                (p/do!
                  (when-let [ns-sym (:ns msg)]
                    (ensure-ns-loaded
                      (select-keys
                        msg
                        [:sparkling/context])
                      server
                      ns-sym))

                  (f server msg)))
              exec/default-scheduler)))


; ======= public interface ================================

(defn message-seq
  "Send the given message to the nrepl server, returning a promise that
   resolves to a lazy sequence of each response message. If *nrepl* has not
   yet been initialized (usually due to *project-config* not yet being
   resolved) this promise will wait for *nrepl* to initialize before
   sending."
  [msg]
  (-> (message* core/message-seq msg)
      (p/then (fn [responses]
                (map (partial parse-message-response msg) responses)))))

(defn message
  "Like (message-seq), but resolves instead to a single value that is the
   combination of all response messages."
  [msg]
  (-> (message* core/message msg)
      (p/then (partial parse-message-response msg))))

(defn evaluate* [opts code-str]
  (-> (message (merge opts
                      {:op :eval
                       :code code-str}))
      (p/then (fn [resp]
                (when-not (:value resp)
                  (throw (ex-info "Failed to evaluate"
                                  {:code code-str
                                   :response resp})))

                (->> resp :value last read-string)))))

(defn- code-with-var-substitution
  [locals code-form]
  (let [symbol->placeholder (zipmap locals
                                    (map (fn [n]
                                           (str "%" n))
                                         (range)))
        placeholder->symbol (map-invert symbol->placeholder)
        with-placeholders (postwalk
                            (fn [form]
                              (or (when-let [s (get symbol->placeholder form)]
                                    (symbol s))
                                  form))
                            code-form)
        the-code `(nrepl/code ~with-placeholders)]
    `(str/replace
       ~the-code
       #"%\d+"
       ~(reduce-kv
          (fn [m p s]
            (assoc m p `(pr-str ~s)))
          {}
          placeholder->symbol))))

(defn ensure-ns [context ns-sym-or-string]
  (let [context (select-keys context [:sparkling/context])
        ns-sym (if (string? ns-sym-or-string)
                 (symbol ns-sym-or-string)
                 ns-sym-or-string)]
    (-> (message* (partial ensure-ns-loaded context) ns-sym)
        (p/then (partial parse-message-response {})))))

(defmacro format-code [locals code-form]
  (code-with-var-substitution locals code-form))

(defmacro evaluate
  ([code-form] `(evaluate nil ~code-form))
  ([opts code-form]
   (let [locals (when (vector? opts)
                  opts)
         opts (when-not (vector? opts)
                opts)]
     (if (nil? locals)
       `(evaluate* ~opts (nrepl/code ~code-form))
       `(evaluate ~opts ~locals ~code-form))))
  ([opts locals code-form]
   (let [the-code (code-with-var-substitution locals code-form)]
     `(evaluate* ~opts ~the-code))))
