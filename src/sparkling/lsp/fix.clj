(ns sparkling.lsp.fix)

(defn ->text-edit [context edit]
  {:changes
   {(:uri context) [{:range (select-keys edit [:start :end])
                     :newText @(:replacement edit)}]}})
