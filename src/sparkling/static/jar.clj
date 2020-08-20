(ns sparkling.static.jar
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn extract-java-boot-classpath-sync []
  (->> (sh "java" "-XshowSettings:properties" "-version")
       :err
       str/split-lines
       (map str/trim)
       (drop-while #(not (str/starts-with? % "sun.boot.class.path")))
       (take-while #(or (str/starts-with? % "sun.boot.class.path")
                        (not (str/includes? % "="))))
       (map (fn [line]
              (if-let [sep (str/index-of line "=")]
                (str/trim (subs line (inc sep)))
                line)))))

(defn extract-jar-classes-sync [path]
  (->> (sh "jar" "tf" path)
       :out
       (re-seq #"(?m)^(.*?)\.class$")
       (map (comp #(str/replace % "/" ".") second))))
