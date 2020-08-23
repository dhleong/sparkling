(ns sparkling.tools.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [sparkling.tools.util :refer [extract-symbol-input]]))

(deftest extract-symbol-input-test
  (testing "Extract simple symbol at middle"
    (is (= "cargo"
           (:match
             (extract-symbol-input
               "(firefly\n(takeoff [cargo"
               12,
               1)))))
  (testing "Extract simple symbol at its start"
    (is (= "cargo"
           (:match
             (extract-symbol-input
               "(firefly\n(takeoff [cargo"
               10,
               1))))))

