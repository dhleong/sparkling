(ns sparkling.tools.util
  (:require [clojure.string :as str]))

(def ^:private identifier-chars "([a-zA-Z._*:$!?+=<>$/-]+)")
(def ^:private identifier-pattern (re-pattern identifier-chars))
(def ^:private regex-identifier-tail (re-pattern (str identifier-chars "$")))
(def ^:private regex-identifier-head (re-pattern (str "^" identifier-chars)))

(defn- input-line [document-text line]
  (-> document-text
      (str/split #"\n" (inc line))
      (get line)))

(defn extract-query-input [document-text character line]
  (let [full-line (input-line document-text line)
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

(defn extract-symbol-input [document-text character line]
  (let [full-line (input-line document-text line)
        identifier-start (reduce
                           (fn [_ index]
                             (when-not (re-matches identifier-pattern
                                                   (subs full-line index (inc index)))
                               (reduced (inc index))))
                           character
                           (range character 0 -1))
        text-line (subs full-line identifier-start)
        [_ match] (re-find regex-identifier-head text-line)]
    {:match match
     :full-line full-line
     :text-line text-line}))
