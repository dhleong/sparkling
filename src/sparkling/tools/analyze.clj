(ns sparkling.tools.analyze
  (:require [promesa.core :as p]
            [rewrite-clj.zip :as rz]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

(defn- analyze-clojure [context relative-path ^String code]
  (nrepl/evaluate
    {:sparkling/context context}
    [relative-path code]
    (let [err (atom nil)
          read-start (atom nil)]

      ; this is based somewhat on nrepl's eval op,
      ; but without the eval step
      (binding [*in* (clojure.lang.LineNumberingPushbackReader.
                       (java.io.StringReader.
                         code))
                *file* relative-path
                *source-path* relative-path]
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
                          (let [input (binding [*read-eval* false]
                                        (read {:read-cond :allow} *in*))]
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

(defn analyze-cljs-eval [context ^String code]
  ; TODO we probably need to ensure we're using the right session
  ; for shadow-cljs, for example
  ; NOTE: our analysis here is based on the presence or absence
  ; of an exception when evaluating. This is probably terrible,
  ; and future work to simply analyze like for clj would be better
  (-> (nrepl/message-seq
        {:op :eval
         :code code
         :file (path/from-uri (:uri context))
         :sparkling/context context})

      (p/then (fn [m]
                ; force evaluating each item until an exception occurs
                (doall m)

                ; but return nil on success
                nil))

      (p/catch (fn [e]
                 (println "Encountered an error eval'ing " context)
                 {:sparkling/exception e}))))

(defn- analyze-cljs-shadow [_context relative-path ^String code]
  ; TODO: get this from the service
  (let [shadow-build-id :app]
    (p/do!
      #_(nrepl/evaluate
          {:sparkling/context context}
          (ns sparkling.analyze
            (:require [cljs.repl]
                      [cljs.js]
                      [cljs.tools.reader.reader-types])))

      (nrepl/evaluate
        ;; {; :sparkling/context context
        ;;  :ns 'sparkling.analyze}
        [relative-path code shadow-build-id]
        (do
          (ns sparkling.analyze)
          (binding [*in* (clojure.lang.LineNumberingPushbackReader.
                           (java.io.StringReader. code)
                           #_1
                           #_relative-path)
                    *file* relative-path
                    *source-path* relative-path]
            (println @#'cider.piggieback/*cljs-compiler-env*)
            (cljs.repl/repl
              (-> (shadow.cljs.devtools.api/get-worker
                    shadow-build-id)
                  :state-ref
                  deref
                  :build-state :compiler-env
                  atom)
              :eval (fn [v]
                      (println v))))))

      nil)))

(defn- cljs-parts [code]
  (loop [n (-> (rz/of-string code {:track-position? true})
               (rz/skip-whitespace))
         forms []]
    (if-not (or (nil? n)
                (rz/whitespace-or-comment? n))
      (recur
        (-> n
            (rz/right)
            (rz/skip-whitespace))
        (conj forms {:position (let [[l c] (rz/position n)]
                                 {:line (dec l)
                                  :character (dec c)})
                     :string (rz/string n)}))

      forms)))

(defn- analyze-cljs-eval-each [context _relative-path code]
  (try
    (->> (cljs-parts code)
         (map
           (fn [{pos :position s :string}]
             (p/let [result (analyze-cljs-eval context s)]
               (when (:sparkling/exception result)
                 (assoc result :sparkling/offset-position pos)))))
         p/all
         (p/map (fn [results]
                  (->> (keep identity results)
                       seq
                       first))))
    (catch clojure.lang.ExceptionInfo e
      ; NOTE: if we caught an exception here, it must have been
      ; from parsing the code at cljs-parts
      (p/resolved {:sparkling/exception e}))))

(defn- analyze-cljs [context relative-path code]
  ;; ; TODO other build tools? be more generic?
  (if (:use-shadow? context)
    ; NOTE: this will never happen right now...
    (analyze-cljs-shadow context relative-path code)

    ; always this:
    (analyze-cljs-eval-each context relative-path code)))

(defn string [uri ^String code]
  (let [relative-path (path/relative uri)
        context {:uri uri}]
    (-> (case (path/extension uri)
          ("clj" "cljc") (analyze-clojure context relative-path code)
          "cljs" (analyze-cljs context relative-path code))
        (p/then' (fn [v]
                   (when v
                     (println "Detected error: " v))
                   v)))))
