(ns sparkling.lsp.fix)

(defn ->text-edit [context edit]
  (println "->text-edit" (pr-str context) (pr-str (dissoc edit :text)))
  (let [new-text @(:replacement edit)]
    (println "  -> new-text=" (pr-str new-text))
    {:changes
     {(:uri context) [{:range (select-keys edit [:start :end])
                       :newText new-text}]}}))
