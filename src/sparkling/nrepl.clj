(ns sparkling.nrepl
  (:require [clojure.set :refer [map-invert]]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]
            [nrepl.core :as nrepl]
            [promesa.core :as p]
            [promesa.exec :as exec]
            [systemic.core :refer [defsys]]
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


; ======= public interface ================================

(defn message
  "Send the given message to the nrepl server, returning a promise that
   resolves to the response. If *nrepl* has not yet been initialized
   (usually due to *project-config* not yet being resolved) this promise
   will wait for *nrepl* to initialize before sending."
  [msg]
  (-> *nrepl*
      (p/then (fn [server]
                (core/message server msg))
              exec/default-scheduler)
      (p/then (fn [resp]
                (if-let [err (:err resp)]
                  (throw (ex-info err
                                  (assoc resp :message msg)))

                  resp)))))

(defn evaluate* [opts code-str]
  (-> (message (merge opts
                      {:op :eval
                       :code code-str}))
      (p/then (fn [resp]
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

(defmacro evaluate
  ([code-form] (evaluate nil code-form))
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
