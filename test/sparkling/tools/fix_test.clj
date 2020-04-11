(ns sparkling.tools.fix-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.tools.fix :refer [parse-error]]))

(deftest parse-error-test
  (testing "Duplicate refer"
    (is (= {:kind :duplicate-refer
            :args ["firefly"]}
           (parse-error "firefly already refers to io.serenity")))))

