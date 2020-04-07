(ns sparkling.nrepl.lsp
  "Coordination from nrepl -> lsp")

(def diagnostic-severity-error 1)

(defn parse-diagnostic [^Throwable e]
  ; TODO
  {:range {:start {:line 0
                   :character 0}
           :end {:line 0
                 :character 0}}
   :source "nrepl"

   :message (ex-message e)

   :severity diagnostic-severity-error})
