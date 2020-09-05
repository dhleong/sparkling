(ns sparkling.tools.edits.on-ns-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.tools.edit :refer [fix->edit]]
            [sparkling.tools.edits.on-ns :refer [insert-refer
                                                 insert-require]]
            [sparkling.util :refer [lit]]))

(deftest insert-refer-test
  (testing "Insert into :require form"
    (let [original (lit "(ns serenity.core"
                        "  (:require [serenity.crew :as crew]))")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace 'serenity.cargo
                  :symbol 'dolls
                  :op insert-refer})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :refer [dolls]]"
                  "            [serenity.crew :as crew]))")
             (-> edit
                 :replacement
                 deref)))
      (is (= {:line 0 :character 0}
             (:start edit)))
      #_(is (= {:line 0 :character (count original)}
             (:end edit)))))

  (testing "Create :require form"
    (let [original (lit "(ns serenity.core)")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace 'serenity.cargo
                  :symbol 'dolls
                  :op insert-refer})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :refer [dolls]]))")
             (-> edit
                 :replacement
                 deref))))))

(deftest insert-require-test
  (testing "Handle strings instead of symbols"
    (let [original (lit "(ns serenity.core)")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace "serenity.cargo"
                  :alias "cargo"
                  :op insert-require})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :as cargo]))")
             @(:replacement edit)))))

  (testing "Create :require form"
    (let [original (lit "(ns serenity.core)")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace 'serenity.cargo
                  :alias 'cargo
                  :op insert-require})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :as cargo]))")
             (-> edit
                 :replacement
                 deref)))
      (is (= {:line 0 :character 0}
             (:start edit)))
      (is (= {:line 0 :character (count original)}
             (:end edit)))))

  (testing "Insert :as to existing :refer"
    (let [original (lit "(ns serenity.core"
                        "  (:require [serenity.cargo :refer [geisha-dolls]]))")]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :as cargo :refer [geisha-dolls]]))")

             (-> (fix->edit
                   {:text original
                    :target 'ns
                    :namespace "serenity.cargo"
                    :alias 'cargo
                    :op insert-require})
             :replacement
             deref)))))

  (testing "Insert new :require entry"
    (let [original (lit "(ns serenity.core"
                        "  (:require [serenity.cargo :as cargo]))")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace 'serenity.apples
                  :alias 'apples
                  :op insert-require})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.apples :as apples]"
                  "            [serenity.cargo :as cargo]))")
             @(:replacement edit)))))

  (testing "Append new :require entry"
    (let [original (lit "(ns serenity.core"
                        "  (:require [serenity.cargo :as cargo]))")
          edit (fix->edit
                 {:text original
                  :target 'ns
                  :namespace 'serenity.crew
                  :alias 'crew
                  :op insert-require})]
      (is (= (lit "(ns serenity.core"
                  "  (:require [serenity.cargo :as cargo]"
                  "            [serenity.crew :as crew]))")
             @(:replacement edit))))))

