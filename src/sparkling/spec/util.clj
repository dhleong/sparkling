(ns sparkling.spec.util
  (:require [clojure.spec.alpha :as s]))

(defn validate [spec obj]
  (if (try (s/valid? spec obj)
           (catch Throwable e
             (throw (ex-info (str "Error validating obj against " spec)
                             {:cause e
                              :obj obj}))))
    obj

    (let [explanation (with-out-str
                        (s/explain spec obj))]
      (throw (ex-info (str "Invalid " spec ": " explanation)
                      {:obj obj
                       :explanation explanation
                       :spec spec})))))
