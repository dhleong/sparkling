(ns sparkling.handlers.text-document
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.handlers.core :refer [defhandler]]
            [sparkling.handlers.text-sync :refer [*doc-state*]]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

(defhandler :textDocument/completion [{{:keys [character line]} :position,
                                       {uri :uri} :textDocument}]
  (p/let [doc (or (get @*doc-state* uri)
                  (throw (IllegalArgumentException.
                           (str "Not opened: " uri))))
          text-line (-> doc
                        (str/split #"\n" (inc line))
                        (get line)
                        (subs 0 character))
          [_ match] (re-find #"([a-zA-Z._*:$!?+=<>$/-]+)$" text-line)

          {:keys [completions]} (nrepl/message
                                  {:op :complete
                                   :ns (path/->ns uri)
                                   :extra-metadata ["arglists" "doc"]
                                   :symbol match})]

    ; TODO arglists? show ns somewhere?
    {:isIncomplete false
     :items (->> completions
                 (map (fn [c]
                        ; TODO kind
                        {:label (:candidate c)
                         :documentation (:doc c)})))}))
