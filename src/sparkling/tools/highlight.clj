(ns sparkling.tools.highlight
  "Semantic color/highlighting tools"
  (:require [promesa.core :as p]
            [sparkling.nrepl :as nrepl]
            [sparkling.path :as path]))

(def ^:private reserved-synax-words
  #{"contains" "oneline" "fold" "display" "extend" "concealends" "conceal"
    "cchar" "contained" "containedin" "nextgroup" "transparent" "skipwhite"'
    "skipnl" "skipempty"})

; special case symbol-type mappings; these might read as macros, but actually
; we want them to be clojureSpecial, for example
(def ^:private specials
  (->> {"clojureSpecial"
        '(def if do let quote var fn loop recur
           monitor-enter monitor-exit . new set!)

        "clojureCond"
        '(case cond cond-> cond->> condp if-let if-not if-some when
           when-first when-let when-not when-some)

        "clojureException"
        '(throw try catch finally)

        "clojureRepeat"
        '(doseq dotimes while)}
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
    - `:?macro`  A symbol that *might* be the fqn of a macro"
  [locals vars-seq]
  (let [multi-fn `#?(:cljs MultiFn
                     :default clojure.lang.MultiFn)]
    `(nrepl/format-code
       ~locals
       (letfn [(fn-ref?# [v#]
                 (or (seq (:arglists (meta v#)))
                     (when-let [derefd# (when (var? v#) @v#)]
                       (or (fn? derefd#)
                           (instance? ~multi-fn derefd#)))))]
         (->> ~vars-seq
           (filter (fn [{the-alias# :alias}]
                     (not (contains? ~reserved-synax-words ~'alias))))
           (map (fn [{:keys [~'var-ref ~'?macro] the-alias# :alias}]
                  (let [m# (meta ~'var-ref)
                        n# (str (or the-alias# (:name m#) ~'?macro))]
                    [n#
                     (or (get ~specials n#)
                         (cond
                           ~'?macro :macro?
                           (:macro m#) :macro
                           (fn-ref?# ~'var-ref) :fn
                           :else :var))])))
           (group-by second)
           (reduce-kv
             (fn [m# kind# entries#]
               (assoc m# kind# (map first entries#)))
             {}))))))

(defn public-vars-for-pairs [context prefix-and-ns-pairs]
  (let [ns-publics-pairs (mapv
                           (fn [[prefix search-ns]]
                             `[~prefix (ns-publics (quote ~(symbol search-ns)))])
                           prefix-and-ns-pairs)
        to-eval (vars->types
                  [ns-publics-pairs]
                  (->> ns-publics-pairs
                       (mapcat (fn [[prefix publics]]
                                 (->> publics
                                      (map (fn [[var-name var-ref]]
                                             {:alias (str prefix var-name)
                                              :var-ref var-ref})))))))]
    (nrepl/evaluate*
      context
      to-eval)))

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
                   :var [(str class-name)]}))
           (apply merge-with concat)))))

(defn types-in [uri ^String _code]
  (let [context {:sparkling/context {:uri uri}}
        search-ns (path/->ns uri)]
    (-> (p/all [(public-vars-for-pairs
                  context
                  [["" search-ns]])

                (aliases context search-ns)
                (imports context search-ns)

                ; TODO ns-refers
                ])
        (p/then' (fn [all-matches]
                   (apply merge-with concat all-matches))))))
