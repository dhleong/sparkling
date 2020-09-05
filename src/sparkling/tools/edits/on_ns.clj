(ns sparkling.tools.edits.on-ns
  (:require [rewrite-clj.zip :as rz]))

(defn- ->sym [v]
  (cond
    (symbol? v) v
    (string? v) (symbol v)
    (keyword? v) (symbol (name v))
    :else (throw (IllegalArgumentException.
                   (str "Unexpected value: " (pr-str v))))))

(defn- belongs-after? [needle candidate]
  (cond
    (rz/vector? candidate)
    (let [required (->> (rz/sexpr candidate)
                        first)]
      (> (compare required needle) 0))

    (symbol? (rz/value candidate))
    (> (compare (rz/value candidate) needle) 0)

    :else (println "what is " candidate)
    ))

(defn- infer-indent [require-form]
  (-> require-form
      (rz/right)
      (rz/position)
      second
      dec))

(defn- value= [expected]
  (fn value=-predicate [v]
    (= expected
       (rz/value v))))


; ======= insert symbol to :refer =========================

(defn- insert-new-refer [zipper to-require sym]
  (some-> zipper
          (rz/find-depth-first (value= to-require))
          (rz/find-next-value :refer)
          (rz/right) ; onto the vector

          (as-> refer-vector
            (if-let [insert-before (rz/find-next-depth-first
                                     refer-vector
                                     (partial belongs-after? sym))]
              ; insert alphabetically
              (rz/insert-left insert-before sym)

              ; just append
              (rz/append-child refer-vector sym)))))


; ======= add eg :as to :refer ============================

(defn- add-verb-to-require [zipper to-require verb object]
  (some-> zipper
          (rz/find-depth-first (value= to-require))

          ; navigate within the :require as appropriate
          (as-> zipper
            (case verb
              :refer (rz/rightmost zipper)

              zipper))

          (rz/insert-right object)
          (rz/insert-right verb)))


; ======= add :require entry ==============================

(defn- add-require-entry [zipper to-require verb object]
  ; TODO if to-require starts with the same top-level ns
  ; that we do, it should be grouped at the end
  (when-let [require-form (rz/find-value zipper rz/next :require)]
    (or (if-let [insert-before (rz/find-next
                                 require-form
                                 (partial belongs-after? to-require))]
          (-> insert-before
              (rz/prepend-space (infer-indent require-form))
              (rz/left*)
              (rz/prepend-newline)
              (rz/left*)
              (rz/insert-left `[~to-require ~verb ~object]))

          ; nothing to insert before; append
          (-> require-form
              (rz/rightmost)
              (rz/append-newline)
              (rz/right*)
              (rz/append-space (infer-indent require-form))
              (rz/right*)
              (rz/insert-right `[~to-require ~verb ~object])))

        ; be non-destructive if we couldn't find a place to insert
        (println "COULD NOT FIND A PLACE TO ADD " [to-require verb object])
        require-form)))

(defn- create-require [zipper to-require verb object]
  (some-> zipper
          (rz/down)
          (rz/rightmost)
          (rz/append-newline)
          (rz/right*)
          (rz/append-space 2)
          (rz/right*)
          (rz/insert-right `(:require
                              [~to-require ~verb ~object]))
          (rz/up)))

(defn insert-refer [{to-require :namespace to-refer :symbol} zipper]
  (let [to-require (->sym to-require)
        to-refer (->sym to-refer)]
    (or

      ; first, if a :refer exists already, insert into it
      (insert-new-refer zipper to-require to-refer)

      ; if not, add a :refer to a :require [:as]
      (add-verb-to-require zipper to-require :refer [to-refer])

      ; if not, try to insert :refer within :require
      (add-require-entry zipper to-require :refer [to-refer])

      ; if no :require, make one!
      (create-require zipper to-require :refer [to-refer])

      ; can't change? don't destroy
      zipper)))

(defn insert-require [{to-require :namespace as :alias} zipper]
  (let [to-require (->sym to-require)
        as (->sym as)]
    (or

      ; first, if it exists as a :refer, insert :as
      (add-verb-to-require zipper to-require :as as)

      ; if not, try to insert within :require
      (add-require-entry zipper to-require :as as)

      ; if no :require, make one!
      (create-require zipper to-require :as as)

      ; can't change? don't destroy
      zipper)))
