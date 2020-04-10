(ns sparkling.config
  (:require [promesa.core :as p]
            [sparkling.builders.deps :as deps]
            [sparkling.builders.shadow :as shadow]
            [systemic.core :refer [defsys]]
            [sparkling.spec.util :refer [validate]]
            [sparkling.spec :as spec]))

; NOTE: a config-filler must accept a ::spec/project-config-minimal and
; either return nil (if nothing could be done) or a ::spec/project-config
(def ^:private config-fillers
  [shadow/fill-project-config
   deps/fill-project-config])

; ======= public interface ================================

(defsys *project-config*
  "This is a Promise that will be resolved to a map matching
   ::spec/project-config"
  :start (p/deferred))

(defn lsp [lsp-key]
  (get-in @*project-config* [:lsp lsp-key]))

(defn fill
  "Given a ::spec/project-config-minimal, attempt to fill it out
   into a proper ::spec/project-config"
  [base]
  (validate ::spec/project-config-minimal base)

  (validate
    ::spec/project-config
    (loop [config base
           fillers config-fillers]
      (if-let [f (first fillers)]
        (recur (or (f config)
                   config)
               (next fillers))
        config))))
