(ns sparkling.tools.edit-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.tools.edit :refer [fix->edit]]
            [sparkling.util :refer [lit]]))

(def ^:private nop (fn [_edit n]
                     n))

(deftest perform-edit-test
  (testing "Simple position detection"
    (let [original (lit "(ns serenity.core)")
          edit (fix->edit
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
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :op nop})]
      (is (= {:line 1 :character 0}
             (:start edit)))
      (is (= {:line 1 :character 18}
             (:end edit)))

      (is (= "(ns serenity.core)"
             @(:replacement edit)))))

  (testing "Shebang-aware namespace targetting"
    (let [original (lit "#!/usr/bin/env bb"
                        "(ns serenity.core)"
                        ""
                        "(println \"Hello world\")")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :op nop})]
      (is (= {:line 1 :character 0}
             (:start edit)))
      (is (= {:line 1 :character 18}
             (:end edit)))

      (is (= "(ns serenity.core)"
             @(:replacement edit)))))

  (testing "Complicated shebang-aware namespace targetting"
    (let [original (lit "#!/usr/bin/env bash"
                        "\"exec\" \"plk\" \"-Sdeps\" \"{:deps {}}\" \"-sf\" \"$0\" \"$@\""
                        "(ns serenity.core)"
                        ""
                        "(println \"Hello world\")")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :op nop})]
      (is (= {:line 2 :character 0}
             (:start edit)))
      (is (= {:line 2 :character 18}
             (:end edit)))

      (is (= "(ns serenity.core)"
             @(:replacement edit))))))

