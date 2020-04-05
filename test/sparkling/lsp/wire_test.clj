(ns sparkling.lsp.wire-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.lsp.wire :refer [parse-in format-out]])
  (:import (okio Buffer)))

(defn format-msg->source [msg]
  (let [source (Buffer.)]
    (format-out source msg)
    source))

(defn format-msg [msg]
  (-> msg
      format-msg->source
      (.readUtf8)))

(deftest parse-test
  (testing "Parse input"
    (let [raw {:method "firefly/liftoff"}
          parsed (parse-in
                   (format-msg->source raw))]
      (is (= {:method "firefly/liftoff"
              :sparkling/headers {:content-length "28"}}
             parsed)))))

