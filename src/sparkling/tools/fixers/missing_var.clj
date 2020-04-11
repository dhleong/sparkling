(ns sparkling.tools.fixers.missing-var
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.tools.fixers.model :refer [def-fixer]]
            [sparkling.tools.resolve :as resolve]))

(def-fixer :missing-var
  {:matches [#"No such var: ([^ ]+)$"]}
  [sym]
  (if-some [index (str/index-of sym "/")]
    ; try fixing ns?
    (p/let [candidates (resolve/preferred-ns-by-alias
                         ; TODO file type
                         :clj
                         (subs sym 0 index))]
      ; TODO
      (println "ns alias candidates=" candidates))

    ; TODO refer?
    ))
