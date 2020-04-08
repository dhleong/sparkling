(ns sparkling.handlers.text-document
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.handlers.text-sync :refer [*doc-state*]]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

(def identifier-chars "([a-zA-Z._*:$!?+=<>$/-]+)")
(def regex-identifier-tail (re-pattern (str identifier-chars "$")))
(def regex-identifier-head (re-pattern (str "^" identifier-chars)))

(defhandler :textDocument/completion [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (or (get @*doc-state* uri)
                  (throw (IllegalArgumentException.
                           (str "Not opened: " uri))))
          full-line (-> doc
                        (str/split #"\n" (inc line))
                        (get line))
          text-line (subs full-line 0 character)
          [_ match] (or (re-find regex-identifier-tail text-line)

                        ; YCM seems to send the first index of eg
                        ; "str/" so let's handle that
                        (re-find regex-identifier-head (subs full-line character)))

          {:keys [completions]} (nrepl/message
                                  {:op :complete
                                   :ns (path/->ns uri)
                                   :extra-metadata ["arglists" "doc"]
                                   :symbol match})]

    (when-not match
      (println "complete at " line ":" character)
      (println "full-line = " full-line)
      (println "text-line = " text-line))

    ; TODO show ns somewhere?
    {:isIncomplete true
     :items (->> completions
                 (map (fn [c]
                        ; TODO kind
                        {:label (:candidate c)

                         :detail (when-let [args (seq (:arglists c))]
                                   (str args))
                         :documentation (:doc c)})))}))
