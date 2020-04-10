(ns sparkling.nrepl.detect-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.nrepl.detect :refer [extract-shadow-builds]]))

(deftest shadow-builds-test
  (testing "#shadow/env reader"
    (is (= [:serenity]
           (extract-shadow-builds
             "{:builds {:serenity #shadow/env \"SERENITY\"}}")))))

