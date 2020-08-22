(ns sparkling.tools.util
  (:require [clojure.string :as str]))

(def identifier-chars "([a-zA-Z._*:$!?+=<>$/-]+)")
(def regex-identifier-tail (re-pattern (str identifier-chars "$")))
(def regex-identifier-head (re-pattern (str "^" identifier-chars)))

(defn extract-query-input [document-text character line]
  (let [full-line (-> document-text
                      (str/split #"\n" (inc line))
                      (get line))
        text-line (subs full-line 0 character)
        [_ match] (or (re-find regex-identifier-tail text-line)

                      (when (re-find #"\($" text-line)
                        ; suggest anything, maybe?
                        [nil ""])

                      ; YCM seems to send the first index of eg
                      ; "str/" so let's handle that
                      (re-find regex-identifier-head (subs full-line character)))]
    {:match match
     :full-line full-line
     :text-line text-line}))

