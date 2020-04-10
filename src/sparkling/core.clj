(ns sparkling.core
  (:require [clojure.java.io :as io]
            [docopt.core :refer [docopt]]
            [sparkling.builders.util :refer [parse-edn]]
            [systemic.core :as systemic]
            [sparkling.config]
            [sparkling.handlers]
            [sparkling.lsp :refer [*lsp*]]
            [sparkling.nrepl])
  (:gen-class))

(defn- run-lsp []
  ; redirect stdout to stderr for debugging
  (alter-var-root #'*out* (constantly *err*))

  (systemic/start!)

  ; wait forever
  @(:promise *lsp*))


; ======= main ============================================

(declare run)

(defn ^{:doc "sparkling

Usage:
  sparkling lsp --stdio
  sparkling -h | --help
  sparkling --version

Options:
  -h --help   Show this message
  --version   Show version"}
  -main
  [& args]
  ; separate fn to satisfy docopt in an uberjar
  (run args))

(defn parse-args [args]
  (docopt args))

(defn run [args]
  (let [>> (parse-args args)]
    (cond
      (or (nil? >>) (>> "--help")) (println (:doc (meta #'-main)))
      (>> "--version") (println (str "sparkling, version "
                                     ; load version from deps.edn
                                     (or (some-> (io/resource "deps.edn")
                                                 slurp
                                                 parse-edn
                                                 :version)
                                         "SNAPSHOT")))

      (>> "lsp") (cond
                   (>> "--stdio") (run-lsp)

                   :else (do
                           (println "sparkling: unsupported lsp mode (try --stdio)")
                           (System/exit 1)))

      :else (do
              (println >>)
              (println "sparkling: invalid")
              (System/exit 1)))))
