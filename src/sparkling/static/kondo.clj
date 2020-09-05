(ns sparkling.static.kondo
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [promesa.core :as p]
            [sparkling.builders.util :refer [parse-edn]]))

(defn index-kondo-analysis [analysis]
  (-> analysis
      (assoc :namespace->contents
             (reduce
               (fn [m var-def]
                 (update m (:ns var-def) conj var-def))
               {}
               (:var-definitions analysis)))))

(defn- kondo-sync [{:keys [config dir]} & args]
  (let [invocation (vec (concat
                          ["clj-kondo" "--cache" "true"
                           "--config" (str (merge-with merge
                                                       {:output {:format :edn}}
                                                       config))]
                          args
                          (when dir
                            [:dir dir])))
        execution (try (apply sh invocation)
                       (catch Exception e
                         (throw (ex-info (str "Failed to invoke clj-kondo: "
                                              invocation)
                                         {:invocation invocation
                                          :cause e}))))]
    (->> execution
         :out
         parse-edn)))

(defn- analyze-sync [dir & args]
  (->> (apply kondo-sync {:dir dir
                          :config {:output {:analysis true}}}
              args)
       :analysis
       index-kondo-analysis))

(defn- analyze-path-sync [dir path]
  (analyze-sync dir "--lint" path))

(defn- analyze-string-sync [s]
  (analyze-sync nil "--lint" "-" :in s))

(defn analyze-path
  ([path] (analyze-path nil path))
  ([dir path]
   {:pre [(not (nil? path))]}
   (p/future (analyze-path-sync dir path))))

(defn analyze-string [s]
  (p/future (analyze-string-sync s)))

(defn lint-string [dir s]
  (p/future (kondo-sync {:dir dir} "--lint" "-" :in s)))

(defn resolve-uri
  "Expects config to be the resolved value of *project-config*"
  [config filename]
  (when-let [path (or (when (.isAbsolute (io/file filename))
                        filename)

                      (let [from-root (io/file (:root-path config)
                                               filename)]
                        (when (.exists from-root)
                          (.getAbsolutePath from-root)))

                      filename)]
    (str "file://" path)))

(defn version []
  (p/future (->> (sh "clj-kondo" "--version")
                 :out
                 str/trimr)))
