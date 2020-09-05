(ns sparkling.tools.fixers.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::description string?)

; the target element within a document for an edit (ex: 'ns)
(s/def ::target symbol?)

; the op to perform; if :target is provided, this should edit
; the text document via zipper (see ex: edits/on-ns). Otherwise,
; it should be a 1-arity fn that accepts the fix object and
; performs some work
(s/def ::op ifn?)

(s/def ::fix-text (s/keys :req-un [::description ::target ::op]))
(s/def ::fix-action (s/keys :req-un [::description ::op]))

(s/def ::fix (s/or :text ::fix-text
                   :action ::fix-action))

(s/def ::fix-result (s/or :single ::fix
                          :multiple (s/coll-of ::fix)))
