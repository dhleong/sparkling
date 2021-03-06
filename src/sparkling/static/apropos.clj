(ns sparkling.static.apropos
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.static :refer [*kondo-classpath* *kondo-project-path*]]
            [sparkling.static.kondo :as kondo]
            [sparkling.util.promise :as promise]))

(defn type-of-kondo [item]
  (cond
    (:fixed-arities item) :function
    :else :var))

(defn- kondo-info->candidate [item]
  {:candidate (:name item)
   :type (type-of-kondo item)
   :doc (or (:doc item)
            (:ns item))
   :ns (:ns item)
   :sparkling/definition item})

(def ^:private xf->candidate
  (comp
    (map kondo-info->candidate)))

; ======= ns-local var defs ===============================

(defn ns-local-definitions [text-analysis-promise]
  (p/let [analysis text-analysis-promise]
    ; TODO query could be:
    ; - an existing namespace alias (in this ns)
    ; - an established namespace alias (from somewhere else in the project)
    ; - a java class name
    ; - a ns/var reference

    (->> (:var-definitions analysis)
         (transduce xf->candidate conj []))))


; ======= var defs in classpath ===========================

(defn var-definitions
  "Return a sequence of all var definitions whose names
   exactly match the given `sym`."
  [_context sym]
  (p/plet [sym (symbol sym)
           cp *kondo-classpath*
           project *kondo-project-path*]
    (->> (concat (:var-definitions cp)
                 (:var-definitions project))
         (transduce
           (comp
             (filter #(= (:name %) sym))
             xf->candidate)
           conj []))))


; ======= var usages ======================================

(defn var-usages [{:keys [text-analysis-promise]}]
  (p/plet [analysis text-analysis-promise
           project *kondo-project-path*]
    (->> analysis
         :var-usages
         (keep (fn [{declared-ns :to var-name :name}]
                 (->> (get-in project
                              [:namespace->contents declared-ns])
                      (filter #(= var-name (:name %)))
                      first)))
         (map kondo-info->candidate))))


; ======= ns aliases ======================================

(def ^:private xf-aliases->map
  (comp
    (filter :alias)))

(defn- assoc-alias->map
  ([m] m)
  ([m ns-usage]
   (assoc m (:alias ns-usage) (:to ns-usage))))

(defn ns-aliases-in [{:keys [text-analysis-promise document-text]}]
  (p/plet [this-ns (or text-analysis-promise
                       (kondo/analyze-string document-text))
           project *kondo-project-path*]
    (->> (concat (:namespace-usages this-ns)
                 (:namespace-usages project))
         (transduce xf-aliases->map assoc-alias->map {}))))


; ======= contents of aliased namespace ===================

(defn ns-alias-contents-query [ctx the-alias]
  (p/let [aliases (promise/with-timing "ns-aliases-in (for contents)"
                    (ns-aliases-in ctx))
          target-ns (get aliases (if (string? the-alias)
                                   (symbol the-alias)
                                   the-alias))
          project-analysis *kondo-classpath*
          in-target-ns (get-in project-analysis [:namespace->contents target-ns])]
    (println "alias contents in  " the-alias "->" target-ns)
    (println "total found " (count (:var-definitions project-analysis)) "defs")
    (println "in target ns " (count in-target-ns))
    (->> in-target-ns

         (map (fn [item]
                {:candidate (str the-alias "/" (:name item))
                 :type (type-of-kondo item)
                 :doc (:doc item)
                 :sparkling/definition item})))))


; ======= primary public interface ========================

(defn static-apropos [{:keys [document-text root-path uri] :as ctx} query]
  (println "apropros: root= " root-path "query=" query)
  (let [ns-separator-idx (str/index-of query "/")
        ns-contents-query? (not (nil? ns-separator-idx))
        local-analysis (promise/with-timing (str "analyze document-text @" uri)
                         (kondo/analyze-string document-text))
        ctx (assoc ctx :text-analysis-promise local-analysis)]
    (->> [(promise/with-timing "ns-local-definitions"
            (ns-local-definitions local-analysis))

          (promise/with-timing "var-usages"
            (var-usages ctx))

          (when-not ns-contents-query?
            (promise/with-timing "ns-aliases-in"
              (p/let [aliases-map (ns-aliases-in ctx)]
                (->> aliases-map
                     (map (fn [[the-alias target-ns]]
                            {:candidate the-alias
                             :type :namespace
                             :doc (str "(:require [" target-ns
                                       " :as " the-alias "])")}))))))

          (when ns-contents-query?
            (promise/with-timing "ns-alias-contents-query"
              (ns-alias-contents-query ctx (subs query 0 ns-separator-idx))))]

         p/all
         (p/map (fn [results]
                  (println results)
                  (->> results
                       (apply concat)
                       (filter #(str/starts-with? (:candidate %) query))
                       distinct))))))
