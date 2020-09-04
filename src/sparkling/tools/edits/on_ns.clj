(ns sparkling.tools.edits.on-ns
  (:require [rewrite-clj.zip :as rz]))

(defn- ->sym [v]
  (cond
    (symbol? v) v
    (string? v) (symbol v)
    (keyword? v) (symbol (name v))
    :else (throw (IllegalArgumentException.
                   (str "Unexpected value: " (pr-str v))))))

(defn- infer-indent [require-form]
  (-> require-form
      (rz/right)
      (rz/position)
      second
      dec))


; ======= add :as to :refer ===============================

(defn- add-as-to-refer [zipper to-require as]
  (some-> zipper
          (rz/find-depth-first (fn [v]
                                 (= to-require
                                    (rz/value v))))
          (rz/insert-right as)
          (rz/insert-right :as)))


; ======= add :require entry ==============================

(defn- belongs-after? [needle candidate]
  (when (rz/vector? candidate)
    (let [required (->> (rz/sexpr candidate)
                        first)]
      (> (compare required needle) 0))))

(defn- add-require-entry [zipper to-require as]
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
              (rz/insert-left `[~to-require :as ~as]))

          ; nothing to insert before; append
          (-> require-form
              (rz/rightmost)
              (rz/append-newline)
              (rz/right*)
              (rz/append-space (infer-indent require-form))
              (rz/right*)
              (rz/insert-right `[~to-require :as ~as])))

        ; be non-destructive if we couldn't find a place to insert
        (println "COULD NOT FIND A PLACE TO ADD " [to-require :as as])
        require-form)))

(defn- create-require [zipper to-require verb contents]
  (some-> zipper
          (rz/down)
          (rz/rightmost)
          (rz/append-newline)
          (rz/right*)
          (rz/append-space 2)
          (rz/right*)
          (rz/insert-right `(:require
                              [~to-require ~verb ~contents]))
          (rz/up)))

(defn insert-refer [{require-ns :namespace to-refer :symbol} zipper]
  (let [require-ns (->sym require-ns)
        to-refer (->sym to-refer)]
    (or

      ; TODO first, if a :refer exists already, insert into it
      ;; (add-as-to-refer zipper to-require as)

      ; TODO if not, try to insert :refer within :require
      ;; (add-require-entry zipper to-require as)

      ; if no :require, make one!
      (create-require zipper require-ns :refer [to-refer])

      ; can't change? don't destroy
      zipper)))

(defn insert-require [{to-require :namespace as :alias} zipper]
  (let [to-require (->sym to-require)
        as (->sym as)]
    (or

      ; first, if it exists as a :refer, insert :as
      (add-as-to-refer zipper to-require as)

      ; if not, try to insert within :require
      (add-require-entry zipper to-require as)

      ; if no :require, make one!
      (create-require zipper to-require :as as)

      ; can't change? don't destroy
      zipper)))
