(ns sparkling.nrepl.lsp
  "Coordination from nrepl -> lsp")

(def ^:private source-name "nrepl:sparkling")
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

(defn- parse-exception [^Throwable e]
  (let [message (ex-message e)
        [l c m] (or (split-error-message message)
                    [0 0 message])]
    {:source source-name
     :severity diagnostic-severity-error

     :message m

     :range {:start {:line l
                     :character c}

             ; TODO: better positioning?
             :end {:line l
                   :character c}}}))

(defn- parse-map [m]
  (let [{:clojure.error/keys [line column]} (:data m)

        ; lsp numbers from 0
        line (dec line)
        column (dec column)]
    {:source source-name
     :severity diagnostic-severity-error

     :message (:cause m)

     :range {:start {:line line
                     :character column}

             ; TODO: better positioning?
             :end {:line line
                   :character column} }
     }))

(defn parse-diagnostic [d]
  (if (ex-message d)
    (parse-exception d)
    (parse-map d)))
