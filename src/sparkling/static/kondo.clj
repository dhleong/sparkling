(ns sparkling.static.kondo
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
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

(defn- analyze-sync [dir & args]
  (let [invocation (vec (concat
                          ["clj-kondo" "--cache" "true"
                           "--config" (str {:output {:analysis true :format :edn}})]
                          args
                          (when dir
                            [:dir dir])))
        execution (try (apply sh invocation)
                       (catch Exception e
                         (throw (ex-info (str "Failed to invoke clj-kondo: " invocation)
                                         {:invocation invocation
                                          :cause e}))))]
    (->> execution
         :out
         parse-edn
         :analysis
         index-kondo-analysis)))

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
