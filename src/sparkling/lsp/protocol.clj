(ns sparkling.lsp.protocol
  (:require [clojure.spec.alpha :as s]))

(def errors {:parse -32700

             :invalid-request -32600
             :method-not-found -32601
             :invalid-params -32602
             :internal -32603

             :cancelled -32800})

(s/def ::line (and int? #(> % 0)))
(s/def ::character (and int? #(> % 0)))
(s/def ::uri string?)

(s/def ::position (s/keys :req-un [::line ::character]))
(s/def ::start ::position)
(s/def ::end ::position)
(s/def ::range (s/keys :req-un [::start ::end]))

(s/def ::newText string?)

(s/def ::text-edit (s/keys :req-un [::range
                                    ::newText]))

(s/def ::changes (s/map-of ::uri (s/coll-of ::text-edit)))
(s/def ::workspace-edit (s/keys :req-un [::changes]))
