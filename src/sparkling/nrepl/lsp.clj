(ns sparkling.nrepl.lsp
  "Coordination from nrepl -> lsp")

(def ^:private source-name "nrepl:sparkling")
(def diagnostic-severity-error 1)

(defn- clean-split-error [[line col msg]]
  [(Integer/parseInt line)
   (Integer/parseInt col)
   msg])

(defn split-clj-error-message [message]
  (some->>
    (or (when-let [[_ & match] (re-find #"at \(.*?:(\d+):(\d+)\).\s*(.*)$"
                                          message)]
          match))
    clean-split-error))

(defn split-shadow-error-message [message]
  (some->>
    (or (when-let [[_ & match] (re-find #"\[line (\d+), col (\d+)\]\s*(.*?)\."
                                        message)]
          match))
    clean-split-error))

(defn- parse-exception [^Throwable e]
  (let [message (ex-message e)
        [l c m] (or (split-clj-error-message message)
                    (split-shadow-error-message message)
                    [0 0 message])]
    ; TODO cleanup
    (println "PARSE " message)
    (println " -> " l c m)
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
                   :character column}} }))

(defn parse-diagnostic [d]
  (cond
    (ex-message d) (parse-exception d)
    (:exception d) (parse-exception (:exception d))
    (map? d) (parse-map d)
    :else (throw (ex-info "Unexpected diagnostic format" d))))
