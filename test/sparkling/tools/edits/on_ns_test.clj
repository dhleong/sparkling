(ns sparkling.tools.edits.on-ns-test
  (:require [clojure.test :refer [deftest is testing]]
            [sparkling.tools.edit :refer [perform-edit]]
            [sparkling.tools.edits.on-ns :refer [insert-require]]
            [sparkling.util :refer [lit]]))

(deftest insert-require-test
  (testing "Create :require form"
    (let [original (lit "(ns serenity.core)")
          edit (perform-edit
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

             (-> (perform-edit
                   {:text original
                    :target 'ns
                    :namespace 'serenity.cargo
                    :alias 'cargo
                    :op insert-require})
             :replacement
             deref))))))

