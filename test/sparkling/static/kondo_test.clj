(ns sparkling.static.kondo-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.static.kondo :refer [index-kondo-analysis]]))

(deftest index-kondo-analysis-test
  (testing "Index var defs by namespace"
    (is (= {'serenity [{:ns 'serenity :name 'firefly}]}

           (:namespace->contents
             (index-kondo-analysis
              {:var-definitions [{:ns 'serenity :name 'firefly}]}))))))

