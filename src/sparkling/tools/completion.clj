(ns sparkling.tools.completion
  (:require [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.apropos :as static]
            [sparkling.nrepl :as nrepl]
            [sparkling.tools.util :refer [extract-query-input]]
            [sparkling.util.promise :as promise]))

(defn- suggest-complete-nrepl
  [{:keys [document-text
           document-ns
           character
           line]
    context :sparkling/context}]
  (p/let [{:keys [match full-line text-line]} (extract-query-input
                                                document-text character line)

          {:keys [completions]} (nrepl/message
                                  {:op :complete
                                   :ns document-ns
                                   :extra-metadata ["arglists" "doc"]
                                   :symbol match
                                   :sparkling/context context})]

    (when (nil? match)
      (println "NO symbol to match: ")
      (println "complete at " line ":" character)
      (println "full-line = " full-line)
      (println "text-line = " text-line)
      (println "head-line = " (subs full-line character)))

    completions))

(defn- suggest-complete-kondo
  [{:keys [document-text
           #_document-ns
           character
           line]}]
  (p/let [config *project-config*
          {:keys [match]} (extract-query-input
                            document-text character line)]
    (static/static-apropos
      {:document-text document-text
       :root-path (:root-path config)}
      match)))

(def ^{:doc "Performs an nrepl :complete request, returning a promise that
             resolves to a sequence of suggestions. Each suggestion is a
             map containing {:candidate, :ns, :type} and optionally
             {:doc, arglists}, as appropriate"}
  suggest-complete
  (promise/fallback
    suggest-complete-nrepl
    suggest-complete-kondo))
