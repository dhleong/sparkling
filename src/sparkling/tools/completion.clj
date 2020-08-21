(ns sparkling.tools.completion
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.apropos :as static]
            [sparkling.nrepl :as nrepl]
            [sparkling.util.promise :as promise]))

(def identifier-chars "([a-zA-Z._*:$!?+=<>$/-]+)")
(def regex-identifier-tail (re-pattern (str identifier-chars "$")))
(def regex-identifier-head (re-pattern (str "^" identifier-chars)))

(defn extract-query-input [document-text character line]
  (let [full-line (-> document-text
                      (str/split #"\n" (inc line))
                      (get line))
        text-line (subs full-line 0 character)
        [_ match] (or (re-find regex-identifier-tail text-line)

                      (when (re-find #"\($" text-line)
                        ; suggest anything, maybe?
                        [nil ""])

                      ; YCM seems to send the first index of eg
                      ; "str/" so let's handle that
                      (re-find regex-identifier-head (subs full-line character)))]
    {:match match
     :full-line full-line
     :text-line text-line}))

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
