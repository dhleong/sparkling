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

(def ^:private re-shadow-line-col
  #"\[line (\d+), col (\d+)\]\s*(.*?)\.")

(def ^:private re-shadow-resource
  #"Resource: [^:]*:(\d+):(\d+)\s*(.*)(?:$|\n|\r)")

(defn split-shadow-error-message [message]
  (some->>
    (or (when-let [[_ & match] (re-find re-shadow-line-col
                                        message)]
          match)

        (when-let [[_ & match] (re-find re-shadow-resource
                                        message)]
          match))
    clean-split-error))

(defn- parse-exception [^Throwable e]
  (let [message (ex-message e)
        [l c m] (or (split-clj-error-message message)
                    (split-shadow-error-message message)
                    [1 1 message])

        ; base-1 -> base-0
        l (dec l)
        c (dec c)]

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

(defn- offset-position [position offset]
  (cond-> position
    true (update :line + (:line offset))
    (= 0 (:line position)) (update :character + (:character offset))))

(defn- offset-positions [parsed offset]
  (if (nil? offset)
    ; no offset; return unchanged
    parsed

    (-> parsed
        (update-in [:range :start] offset-position offset)
        (update-in [:range :end] offset-position offset))))

(defn parse-diagnostic [d]
  (prn "parse " d)
  (-> (cond
        (ex-message d) (parse-exception d)
        (:sparkling/exception d) (parse-exception (:sparkling/exception d))
        (map? d) (parse-map d)
        :else (throw (ex-info "Unexpected diagnostic format" {:d d})))

      (offset-positions (:sparkling/offset-position d))))
