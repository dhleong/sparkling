(ns sparkling.nrepl-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.nrepl :refer [format-value]]))

(deftest format-value-test
  (testing "Format string"
    (is (= "\"mal\nreynolds\""
           (format-value "mal\nreynolds")))))

