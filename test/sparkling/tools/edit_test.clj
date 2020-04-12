(ns sparkling.tools.edit-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.tools.edit :refer [perform-edit]]
            [sparkling.util :refer [lit]]))

(def ^:private nop (fn [_edit n]
                     n))

(deftest perform-edit-test
  (testing "Simple position detection"
    (let [original (lit "(ns serenity.core)")
          edit (perform-edit
                 {:text original
                  :target 'ns
                  :op nop})]
      (is (= {:line 0 :character 0}
             (:start edit)))
      (is (= {:line 0 :character 18}
             (:end edit)))))

  (testing "Whitespace-aware namespace targetting"
    (let [original (lit "; silly comment"
                        "(ns serenity.core)"
                        ""
                        "(println \"Hello world\")")
          edit (perform-edit
                 {:text original
                  :target 'ns
                  :op nop})]
      (is (= {:line 1 :character 0}
             (:start edit)))
      (is (= {:line 1 :character 18}
             (:end edit))))))

