(ns sparkling.lsp.dispatcher-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.lsp.dispatcher :refer [method->kw]]))

(deftest method->kw-test
  (testing "Un-namespaced"
    (is (= :takeoff
           (method->kw "takeoff"))))

  (testing "Un-namespaced"
    (is (= :serenity/takeoff
           (method->kw "serenity/takeoff")))))

