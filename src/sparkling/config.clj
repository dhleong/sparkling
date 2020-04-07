(ns sparkling.config
  (:require [promesa.core :as p]
            [systemic.core :refer [defsys]]))

(defsys *project-config*
  "This is a Promise that will be resolved to a map matching
   ::spec/project-config"
  :start (p/deferred))

(defn lsp [lsp-key]
  (get-in @*project-config* [:lsp lsp-key]))
