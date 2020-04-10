(ns sparkling.path
  (:require [clojure.string :as str]
            [sparkling.config :refer [*project-config*]]))

(defn extension [uri]
  (when uri
    (subs uri (inc (str/last-index-of uri ".")))))

(defn from-uri [uri]
  (subs uri (count "file://")))

(defn relative
  ([uri] (relative @*project-config* uri))
  ([_project-config uri]
   ; FIXME: source roots
   (let [src (str/index-of uri "/src")]
     (subs uri (+ src (count "/src"))))
   ))

(defn ->ns
  ([uri] (->ns @*project-config* uri))
  ([project-config uri]
   (let [{:keys [root-path]} project-config

         ; FIXME: we need to detect source roots
         ns-part (subs uri
                       (+ (str/index-of uri root-path)
                          (count root-path)))]
     (-> ns-part
         (str/replace "src/" "") ;; hacks

         (str/replace #"^/" "")
         (str/replace #"\.\w+$" "") ;; strip extension

         (str/replace #"[/\\_]"
                      (fn [^String c]
                        (case (.charAt c 0)
                          \\ "."
                          \/ "."
                          \_ "-")))))))
