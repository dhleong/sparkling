(ns sparkling.lsp.fix)

(defn ->text-edit [context edit]
  (let [new-text @(:replacement edit)]
    {:title (:description edit)
     :changes
     {(:uri context) [{:range (select-keys edit [:start :end])
                       :newText new-text}]}}))
