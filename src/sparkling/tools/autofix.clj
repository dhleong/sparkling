(ns sparkling.tools.autofix
  (:require [promesa.core :as p]
            [sparkling.static.kondo :as kondo]))

(defn find-fixes [{:keys [document-text
                          root-path
                          line]}]
  (p/let [lints (kondo/lint-string root-path document-text)]
    (println "LINTS = " (pr-str lints))
    (println "at line? " (filter #(= line (:row %)) lints))))
