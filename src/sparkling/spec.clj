(ns sparkling.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::root-path string?)

(s/def ::project-config (s/keys :req-un [::root-path]))
