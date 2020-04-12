(ns sparkling.path
  (:require [clojure.string :as str]
            [sparkling.config :refer [*project-config*]]))

(defn extension [uri]
  (when uri
    (subs uri (inc (str/last-index-of uri ".")))))

(defn ->file-type [uri]
  (when-let [ext (extension uri)]
    (keyword ext)))

(defn from-uri [uri]
  (subs uri (count "file://")))

(defn relative
  ([uri] (relative @*project-config* uri))
  ([project-config uri]
   (let [root-path (:root-path project-config)
         from-root (let [root (str/index-of uri root-path)]
                     (subs uri (+ root (count root-path))))]
     (try
       (if-let [source-paths (:source-paths project-config)]
         (->> source-paths
              (keep (fn [path]
                      (when-some [idx (str/index-of from-root path)]
                        (subs from-root (+ idx (count path))))))
              first)

         ; no explicit source paths? try a generic fallback
         (if-some [src (str/index-of from-root "/src")]
           (subs from-root (+ src (count "/src")))
           from-root))
       (catch Exception e
         (throw (IllegalArgumentException.
                  (pr-str "Failed to determine relative path of " uri " in " project-config)
                  e)))))))

(defn ->ns
  ([uri] (->ns @*project-config* uri))
  ([project-config uri]
   (-> (relative project-config uri)

       (str/replace #"^/" "")
       (str/replace #"\.\w+$" "") ;; strip extension

       (str/replace #"[/\\_]"
                    (fn [^String c]
                      (case (.charAt c 0)
                        \\ "."
                        \/ "."
                        \_ "-"))))))
