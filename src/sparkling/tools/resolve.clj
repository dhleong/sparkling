(ns sparkling.tools.resolve
  (:require [promesa.core :as p]
            [sparkling.nrepl :as nrepl]))

(defn preferred-ns-by-alias
  "Attempt to resolve the *preferred* namespace for a given alias.

   `source-type` should be one of :clj, :cljs, etc."
  [source-type ns-alias]
  (println "resolve " ns-alias "for" source-type "...")
  (p/let [{by-type :namespace-aliases} (nrepl/message
                                         {:op :namespace-aliases
                                          :serialization-format :bencode})
          for-type (get by-type (or source-type :clj))]

    ; NOTE: with the :bencode
    (get for-type (keyword ns-alias))))
