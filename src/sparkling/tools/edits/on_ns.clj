(ns sparkling.tools.edits.on-ns
  (:require [rewrite-clj.zip :as rz]))

(defn insert-require [{to-require :namespace as :alias} zipper]
  (or

    ; first, if it exists as a :refer, insert :as
    (some-> zipper
            (rz/find-depth-first (fn [v]
                                   (= to-require
                                      (rz/value v))))
            (rz/insert-right as)
            (rz/insert-right :as))

    ; if not, try to insert within :require
    ; TODO

    ; if no :require, make one!
    (some-> zipper
            (rz/down)
            (rz/rightmost)
            (rz/append-newline)
            (rz/right*)
            (rz/append-space 2)
            (rz/right*)
            (rz/insert-right `(:require
                                [~to-require :as ~as]))
            (rz/up))

    ; can't change? don't destroy
    zipper))
