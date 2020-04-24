(ns sparkling.tools.highlight
  "Semantic color/highlighting tools"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

; TODO check if we can omit this now (it's a special case for vim)
; and/or move it out of core
(def ^:private reserved-syntax-words
  #{"contains" "oneline" "fold" "display" "extend" "concealends" "conceal"
    "cchar" "contained" "containedin" "nextgroup" "transparent" "skipwhite"'
    "skipnl" "skipempty"})

; special case symbol-type mappings; these might read as macros, but actually
; we want them to be clojureSpecial, for example
(def ^:private specials
  (->> {:special '(def if do let quote var fn loop recur
                    monitor-enter monitor-exit . new set!)

        :cond '(case cond cond-> cond->> condp if-let if-not if-some when
                 when-first when-let when-not when-some)

        :exception '(throw try catch finally)

        :repeat '(doseq dotimes while)}

       (reduce-kv (fn [m kind entries]
                    (merge m
                           (zipmap (map str entries)
                                   (repeat kind))))
                  {})))

(defmacro ^:private vars->types
  "Helper that, given a vec of locals and a form that evaluates to a
   sequence of var-info objects (like nrepl/evaluate), returns a string
   representation of eval'able code to generate a map of type -> var
   names.

   A var-info object should have at least one of the following keys:
    - `:var-ref` A Var instance
    - `:alias`   String; how the var is refer'd in the calling ns
    - `:macro?`  A symbol that *might* be the fqn of a macro"
  [context locals vars-seq]
  (let [multi-fn (symbol "#?(:cljs MultiFn
                             :default clojure.lang.MultiFn)")]
    `(nrepl/evaluate*
       ~context
       (nrepl/format-code
         ~locals
         (letfn [(fn-ref?# [v#]
                   (or (seq (:arglists (meta v#)))
                       (when-let [derefd# (when (var? v#) @v#)]
                         (or (fn? derefd#)
                             (instance? ~multi-fn derefd#)))))]
           (->> ~vars-seq
             (remove (fn [{the-alias# :alias}]
                       (contains? ~reserved-syntax-words the-alias#)))
             (map (fn [{:keys [~'var-ref ~'macro?] the-alias# :alias}]
                    (let [m# (meta ~'var-ref)
                          n# (str (or the-alias# (:name m#) ~'macro?))]
                      [n#
                       (or (get ~specials n#)
                           (cond
                             ~'macro? :macro?
                             (:macro m#) :macro
                             (fn-ref?# ~'var-ref) :fn
                             :else :var))])))
             (group-by second)
             (reduce-kv
               (fn [m# kind# entries#]
                 (assoc m# kind# (map first entries#)))
               {})))))))

(defn public-vars-for-pairs [context prefix-and-ns-pairs]
  (let [ns-publics-pairs (mapv
                           (fn [[prefix search-ns]]
                             `[~prefix (ns-publics (quote ~(symbol search-ns)))])
                           prefix-and-ns-pairs)]
    (vars->types
      context
      [ns-publics-pairs]
      (->> ns-publics-pairs
           (mapcat (fn [[prefix publics]]
                     (->> publics
                          (map (fn [[var-name var-ref]]
                                 {:alias (str prefix var-name)
                                  :var-ref var-ref})))))))))

(defn aliases [context search-ns]
  (-> (nrepl/message
        (merge context
               {:op :ns-aliases
                :ns search-ns}))
      (p/then (fn [{result :ns-aliases}]
                (when result
                  (public-vars-for-pairs
                    context
                    (->> result
                         (map (fn [[alias-kw fqn]]
                                [(str (name alias-kw)
                                      "/")
                                 fqn])))))))))

(defn imports [context search-ns]
  (let [search-ns-sym (symbol search-ns)]
    (nrepl/evaluate
      context
      [search-ns-sym]
      (->> (ns-imports (quote search-ns-sym))
           (mapcat (fn [[class-name klass]]
                     [class-name
                      (when-not (ifn? klass)
                        ; in cljs, we don't have `class?`, but imports
                        ; will be IFns instead; we cannot, sadly, use
                        ; the #?(:cljs) syntax here...
                        (.getName klass))]))
           (keep identity)
           (map (fn [class-name]
                  {:fn [(str class-name ".")]
                   :class [(str class-name)]}))
           (apply merge-with concat)))))

(defn- refers-clj [context search-ns]
  (let [search-ns-sym (symbol search-ns)]
    (vars->types
      context
      [search-ns-sym]
      (->> (ns-refers (quote search-ns-sym))
           (map second)
           (filter #(not= "clojure.core"
                          (->> % meta :ns)))
           (map (fn [var-ref]
                  {:alias (-> var-ref meta :name)
                   :var-ref var-ref}))))))

(defn- parse-cljs-ns-form [context ns-form]
  ; NOTE: it'd be nice to avoid a roundtrip here and just
  ; parse locally, but the cljs.analyzer works so well, and
  ; clojure.tools just returns an AST. Future work could
  ; certainly enhance this...
  (nrepl/evaluate
    context
    [ns-form]
    (let [parsed (binding [cljs.env/*compiler* (atom nil)]
                   (cljs.analyzer/parse
                     'ns
                     (cljs.analyzer/empty-env)
                     (quote ns-form)))]
      (->> (concat
             (:use-macros parsed)
             (:uses parsed))
           (map (fn [[var-name var-ns]]
                  (str var-ns "/" var-name)))))))

(defn- refers-cljs-by-parse [context code]
  ; NOTE there must be a better way to handle this, but the empty analyzer
  ; env doesn't seem to handle things like clojure.core.async (it barfs with
  ; a warning that it doesn't exist) but cljs.core.async can be used just
  ; fine (as with other clojure.core properties).
  (p/let [ns-form (-> code
                      (str/replace "[clojure." "[clj.")
                      (edn/read-string))
          _ (nrepl/ensure-ns context 'cljs.analyzer)
          symbol-names (parse-cljs-ns-form context ns-form)

          ; NOTE: since accessing var metadata and things like
          ; (ns-publics) are compile-time *only* in clojurescript, we have
          ; to first fetch the symbols (above), then issue *another*
          ; request to get their info; here we're building the (resolve)
          ; form for each sybol so the code it can run at compile time
          ; with the quoted symbol.
          symbols (->> symbol-names
                       (mapv (fn [symbol-name]
                               (let [sym (symbol symbol-name)]
                                 `(if-let [var-ref# (resolve (quote ~sym))]
                                    {:var-ref var-ref#}
                                    {:macro? (quote ~sym)})))))]

    (vars->types
      context
      [symbols]
      symbols)))

(defn- resolve-macro-candidates
  "Given a collection of namespaced symbols that couldn't be resolved in
   the cljs context, attempt to resolve them in the clj context"
  [candidates]
  (let [quoted-candidates (mapv (fn [candidate]
                                  `{:var-ref (resolve
                                               (quote
                                                 ~(symbol candidate)))})
                                candidates)]
    (vars->types
      nil
      [quoted-candidates]
      quoted-candidates)))

(defn- refers-cljs [context code]
  (p/let [referred (refers-cljs-by-parse context code)
          macros (when-let [{:keys [macro?]} referred]
                   (resolve-macro-candidates macro?))]
    (merge (dissoc referred :macro?) macros)))

(defn refers [context search-ns code]
  (when (= "clj" (path/extension (-> context :sparkling/context :uri)))
    (refers-clj context search-ns)

    ; NOTE: cljs doesn't support ns-refers support
    (refers-cljs context code)))

(defn types-in [uri ^String code]
  (let [context {:sparkling/context {:uri uri}}
        search-ns (path/->ns uri)]
    (-> (p/all [(public-vars-for-pairs
                  context
                  [["" search-ns]])

                (aliases context search-ns)
                (imports context search-ns)
                (refers context search-ns code)])

        (p/then' (fn [all-matches]
                   (apply merge-with concat all-matches))))))
