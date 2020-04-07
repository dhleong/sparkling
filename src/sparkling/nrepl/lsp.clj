(ns sparkling.nrepl.lsp
  "Coordination from nrepl -> lsp")

(def diagnostic-severity-error 1)

(defn- clean-split-error [[line col msg]]
  [(Integer/parseInt line)
   (Integer/parseInt col)
   msg])

(defn split-error-message [message]
  (some->>
    (or (when-let [[_ & match] (re-find #"at \(.*?:(\d+):(\d+)\).\s*(.*)$"
                                          message)]
          match))
    clean-split-error))

(defn parse-diagnostic [^Throwable e]
  (let [message (ex-message e)
        [l c m] (or (split-error-message message)
                    [0 0 message])]
    {:source "nrepl:sparkling"
     :severity diagnostic-severity-error

     :message m

     :range {:start {:line l
                     :character c}

             ; TODO: better positioning?
             :end {:line l
                   :character c}}}))
