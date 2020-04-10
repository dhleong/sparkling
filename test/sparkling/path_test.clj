(ns sparkling.path-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.path :as path]))

(def root-path "/Users/mreynolds/projects/serenity")
(def simple-config {:root-path root-path})
(def uri-root (str "file://" root-path))

(defn- config-with-paths [& paths]
  (merge simple-config
         {:source-paths paths}))

(deftest ->ns-test
  (testing "Simple src/ path"
    (is (= "serenity.core"
           (path/->ns simple-config
                      (str uri-root "/src/serenity/core.clj")))))

  (testing "src/main path"
    (let [config (config-with-paths "src/main" "src/test")]
      (is (= "serenity.core"
             (path/->ns config
                        (str uri-root "/src/main/serenity/core.clj"))))
      (is (= "serenity.core-test"
             (path/->ns config
                        (str uri-root "/src/test/serenity/core_test.clj")))))))

