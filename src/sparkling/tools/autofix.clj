(ns sparkling.tools.autofix
  (:require [promesa.core :as p]
            [sparkling.static.kondo :as kondo]
            [sparkling.util.promise :as promise]))

(defn find-diagnostics [{:keys [document-text
                                root-path
                                line]}]
  (p/let [lints (promise/with-timing "lint document-text"
                  (kondo/lint-string root-path document-text))
          findings (:findings lints)]
    (println "LINTS = " (pr-str findings))
    (println "at line? " (filter #(= line (:row %)) lints))
    (->> findings
         (map (fn [lint]
                {:message (:message lint)})))))
