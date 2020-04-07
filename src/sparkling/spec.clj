(ns sparkling.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::did-save? boolean?)
(s/def ::lsp (s/keys :req-un [::did-save?]))

(s/def ::root-path string?)

(s/def ::project-config (s/keys :req-un [::root-path]
                                :opt-un [::lsp]))
