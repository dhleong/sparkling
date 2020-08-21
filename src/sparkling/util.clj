(ns sparkling.util
  (:require [clojure.java.io :as io]
            [sparkling.builders.util :refer [parse-edn]]))

(def version (memoize
               #(or (some-> (io/resource "deps.edn")
                            slurp
                            parse-edn
                            :version)
                    "SNAPSHOT")))
