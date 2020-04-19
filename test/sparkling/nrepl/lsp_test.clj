(ns sparkling.nrepl.lsp-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [sparkling.nrepl.lsp :refer [split-shadow-error-message
                                         parse-diagnostic]]
            [sparkling.util :refer [lit]]))

(def ^:private no-such-namespace
  (lit "------ WARNING - :undeclared-ns ------------------------------------------------"
       " Resource: :40:2"
       " No such namespace: alliance, could not locate alliance.cljs, alliance.cljc, or JavaScript source providing \"alliance\""
       "--------------------------------------------------------------------------------"))

(deftest split-shadow-error-message-test
  (testing "Error with Resource tag"
    (is (= [40 2 "No such namespace: alliance, could not locate alliance.cljs, alliance.cljc, or JavaScript source providing \"alliance\""]
           (split-shadow-error-message
             no-such-namespace)))))

(deftest parse-diagnostic-test
  (testing "Apply position offset"
    (is (= {:line 79
            :character 1}
           (-> {:sparkling/exception (ex-info no-such-namespace {})
                :sparkling/offset-position {:line 40 :character 30}}
               parse-diagnostic
               :range
               :start)))

    ; errors on the first line should get offset by :character
    (is (= {:line 40
            :character 31}
           (-> {:sparkling/exception (ex-info
                                       (-> no-such-namespace
                                           (str/replace ":40" ":1"))
                                       {})
                :sparkling/offset-position {:line 40 :character 30}}
               parse-diagnostic
               :range
               :start)))))
