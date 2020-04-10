(ns sparkling.builders.util
  (:require [clojure.edn :as edn]))

(def parse-edn (partial edn/read-string {:default #(do %2)}))


