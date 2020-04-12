(ns sparkling.tools.edit
  (:require [clojure.spec.alpha :as s]
            ;; [rewrite-clj.parser :as rp]
            [clojure.string :as str]
            [rewrite-clj.zip :as rz]
            [sparkling.spec.util :refer [validate]]))

(s/def ::text string?)
(s/def ::target symbol?)
(s/def ::op ifn?)

(s/def ::spec (s/keys :req-un [::text
                               ::target
                               ::op]))

(defn- node-position [n]
  (try
    (when n
      (let [[line character] (rz/position n)]
        ; NOTE: rz/position is 1's-based
        {:line (dec line)
         :character (dec character)}))
    (catch IllegalStateException e
      (println "WARN: could not get position: " (ex-message e))
      nil)))

(defn perform-edit [edit]
  (validate ::spec edit)

  (let [document (rz/of-string (:text edit) {:track-position? true})
        target (-> document
                   (rz/find-value rz/next (:target edit))
                   (rz/up))
        start (node-position target)]

    {:replacement (delay
                    (-> target
                        (rz/subedit-node (partial (:op edit) edit))
                        (rz/root-string)))

     :start start
     :end (or (some-> target
                      (rz/find rz/right* rz/whitespace-or-comment?)
                      node-position)

              (let [node-text (rz/string target)
                    lines (->> node-text
                               (filter #(= \newline %))
                               count)]
                (if (= lines 0)
                  {:line (:line start)
                   :character (count node-text)}

                  (let [last-newline (str/last-index-of node-text "\n")
                        last-line-length (- (count node-text)
                                            last-newline)]
                    {:line (+ (:line start) lines)
                     :character last-line-length}))))}))
