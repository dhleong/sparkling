(ns sparkling.util
  (:require [clojure.string :as str]))

(defn lit
  "Convenience for creating a literal multiline string.
   Clojure supports these of course, but using this keeps
   indentations ane"
  [& lines]
  (str/join "\n" lines))

