(ns sparkling.static.apropos
  (:require [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.static :refer [*kondo-classpath*]]
            [sparkling.static.kondo :as kondo]))

(defn type-of-kondo [item]
  (cond
    (:fixed-arities item) :function
    :else :var))

; ======= ns-local var defs ===============================

(defn ns-local-definitions [document-text]
  (p/let [analysis (kondo/analyze-string document-text)]
    ; TODO query could be:
    ; - an existing namespace alias (in this ns)
    ; - an established namespace alias (from somewhere else in the project)
    ; - a java class name
    ; - a ns/var reference

    (for [item (:var-definitions analysis)]
      {:candidate (:name item)
       :type (type-of-kondo item)
       :doc (:ns item)})))


; ======= ns aliases ======================================

(def ^:private xf-aliases->map
  (comp
    (filter :alias)))

(defn- assoc-alias->map
  ([m] m)
  ([m ns-usage]
   (assoc m (:alias ns-usage) (:to ns-usage))))

(defn ns-aliases-in [{:keys [document-text root-path]}]
  (p/plet [this-ns (kondo/analyze-string document-text)
           project (kondo/analyze-path root-path)]
    (->> (concat (:namespace-usages this-ns)
                 (:namespace-usages project))
         (transduce xf-aliases->map assoc-alias->map {}))))


; ======= contents of aliased namespace ===================

(defn ns-alias-contents-query [ctx the-alias]
  (p/let [aliases (ns-aliases-in ctx)
          target-ns (get aliases (if (string? the-alias)
                                   (symbol the-alias)
                                   the-alias))
          project-analysis *kondo-classpath*]
    (println "alias contents in  " the-alias)
    (println "total found " (count (:var-definitions project-analysis)) "defs")
    (->> project-analysis
         :var-definitions
         (filter #(= target-ns (:ns %)))
         (map (fn [item]
                {:candidate (str the-alias "/" (:name item))
                 :type (type-of-kondo item)
                 :doc (:doc item)
                 ::raw item })))))


; ======= primary public interface ========================

(defn static-apropos [{:keys [document-text root-path] :as ctx} query]
  (println "apropros: root= " root-path "query=" query)
  (let [ns-separator-idx (str/index-of query "/")
        ns-contents-query? (not (nil? ns-separator-idx))]
    (->> [(ns-local-definitions document-text)

          (when-not ns-contents-query?
            (p/let [aliases-map (ns-aliases-in ctx)]
              (->> aliases-map
                   (map (fn [[the-alias target-ns]]
                          {:candidate the-alias
                           :type :namespace
                           :doc (str target-ns)})))))

          (when ns-contents-query?
            (ns-alias-contents-query ctx (subs query 0 ns-separator-idx)))]

         p/all
         (p/map (fn [results]
                  (apply concat results))))))
