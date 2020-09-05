(ns sparkling.tools.edit
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [rewrite-clj.zip :as rz]
            [sparkling.tools.fixers.spec :as fixers]
            [sparkling.spec.util :refer [validate]]))

(s/def ::text string?)

(s/def ::spec (s/and (s/keys :req-un [::text])
                     ::fixers/fix))

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

(defn- subedit-node
  "Inlined version of rz/subedit-node that doesn't lose :track-position?"
  [zloc f]
  (let [zloc' (some-> zloc rz/node (rz/edn* {:track-position? true}))
        _ (assert zloc' "could not create subzipper.")
        zloc' (f zloc')]
    (rz/replace zloc (rz/root zloc'))))

(defn- skip-comments [text]
  (loop [offset 0
         lines 0]
    (if (= \( (get text offset))
      ; found!
      [lines (subs text offset)]

      (if-let [next-offset (str/index-of text "\n" (inc offset))]
        (recur (inc next-offset)
               (inc lines))

        [lines text]))))

(defn- with-line-offset [pos line-offset]
  (update pos :line + line-offset))

(defn fix->edit [fix]
  (validate ::spec fix)

  (if-let [target-element (:target fix)]
    (let [[line-offset text] (skip-comments (:text fix))
          document (rz/of-string text {:track-position? true})
          target (-> document
                     (rz/find-value rz/next target-element)
                     (rz/up))
          start (with-line-offset (node-position target) line-offset)]

      {:description (:description fix) ; forward this along

       :replacement (delay
                      (-> target
                          (subedit-node (partial (:op fix) fix))
                          (rz/string)))

       :start start
       :end (or (some-> target
                        (rz/find rz/right* rz/whitespace-or-comment?)
                        node-position
                        (with-line-offset line-offset))

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
                       :character last-line-length}))))})

    ; just invoke the fix :op directly, I guess?
    (do
      ((:op fix) fix)

      ; and return nil from it; there's no text edit
      nil)))
