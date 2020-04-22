(ns sparkling.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::did-save? boolean?)
(s/def ::lsp (s/keys :req-un [::did-save?]))

(s/def ::root-path string?)
(s/def ::source-paths (s/coll-of string?))
(s/def ::user-ns (s/or :string string?
                       :symbol symbol?))

(s/def ::project-config-minimal (s/keys :req-un [::root-path]
                                        :opt-un [::lsp]))

(s/def ::project-config (s/and ::project-config-minimal
                               (s/keys :opt-un [::source-paths
                                                ::user-ns])))
