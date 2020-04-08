(ns sparkling.path-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.path :as path]))

(def root-path "/Users/mreynolds/projects/serenity")
(def simple-config {:root-path root-path})
(def uri-root (str "file://" root-path))

(deftest ->ns-test
  (testing "Simple src/ path"
    (is (= "serenity.core"
           (path/->ns simple-config
                      (str uri-root "/src/serenity/core.clj"))))))

