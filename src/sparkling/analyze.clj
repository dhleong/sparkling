(ns sparkling.analyze
  (:require [clojure.main :as main]
            [sparkling.nrepl :as nrepl]))

(defn- analyze-clojure [^String code]
  (nrepl/evaluate
    [code]
    (let [err (atom nil)
          read-start (atom nil)]

      ; this is based somewhat on nrepl's eval op,
      ; but without the eval step
      (binding [*in* (clojure.lang.LineNumberingPushbackReader.
                       (java.io.StringReader.
                         code))]
        (clojure.main/repl
          :eval (fn [v]
                  (try
                    (. clojure.lang.Compiler
                       (analyze clojure.lang.Compiler$C/EVAL v))
                    (catch Throwable e
                      ; if we encounter any parse error, ensure that
                      ; we don't keep trying to read anything else
                      (.skip *in* Long/MAX_VALUE)
                      (reset! err {:cause e
                                   :form v})))

                  ; NOTE: eval ns forms to build some context
                  (when (and (seq? v)
                             (= 'ns (first v)))
                    (eval v)))

          :read (fn read-form [request-prompt request-exit]
                  ; NOTE: the default implementation resets the line/col
                  ; for each form; we want to preserve that, since we're
                  ; reading a whole file
                  (or ({:line-start request-prompt :stream-end request-exit}
                       (clojure.main/skip-whitespace *in*))
                      (let [start-line (reset! read-start (.getLineNumber *in*))]
                        (try
                          (let [input (read {:read-cond :allow} *in*)]
                            (clojure.main/skip-if-eol *in*)
                            input)
                          (catch Throwable e
                            ; attach more accurate location info to
                            ; syntax read errors
                            (.skip *in* Long/MAX_VALUE)
                            (reset! err {:cause e
                                         :start-line start-line}))))))

          :caught (fn [^Throwable e]
                    (reset! err {:cause e}))))

      (when-let [data @err]
        (let [{e :cause :keys [form start-line]} data

              root (or (clojure.main/root-cause e)
                       e)]
          {:message (or (ex-message e)
                        (ex-message root))
           :cause (ex-message (ex-cause e))
           :root (ex-message root)
           :form (str form)
           :meta (str (meta form))
           :data (merge
                   {:clojure.error/line (or start-line
                                            @read-start)
                    :clojure.error/column 1}

                   (ex-data e))})))))

(defn string [_uri ^String code]
  (analyze-clojure code))