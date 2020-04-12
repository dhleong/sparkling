(ns sparkling.tools.resolve
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

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

(defn missing-var
  [context sym]
  (if-some [index (str/index-of sym "/")]
    ; try fixing ns?
    (p/let [the-alias (subs sym 0 index)
            candidates (preferred-ns-by-alias
                         (path/->file-type (:uri context))
                         the-alias)]
      (when (seq candidates)
        ; TODO
        (println "ns alias candidates=" candidates)
        {:type :ns
         :alias the-alias
         :candidates candidates}))

    ; TODO refer?
    ))
