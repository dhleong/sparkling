(ns sparkling.builders.shadow-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.builders.shadow :as shadow]))

(deftest shadow-builds-test
  (testing "#shadow/env reader"
    (is (= [:serenity]
           (->> "{:builds {:serenity #shadow/env \"SERENITY\"}}"
                shadow/parse-config
                shadow/extract-builds)))))

