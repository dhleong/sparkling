(ns sparkling.lsp.wire
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.string :as str])
  (:import (okio Sink Source)))

(def ^:dynamic *json-options* {})

(defn parse-in [^Source source]
  (loop [headers {}]
    (let [line (.readUtf8Line source)]
      (if-not (empty? line)
        (recur (let [separator (.indexOf line ":")
                     header-name (subs line 0 separator)
                     header-value (subs line (inc separator))]
                 (assoc headers
                        (keyword (str/lower-case header-name))
                        (str/trim header-value))))

        (let [content-string (.readUtf8 source (Long/parseLong (:content-length headers)))
              content (parse-string content-string true)]
          (assoc content :sparkling/headers headers))))))

(defn- write-str [^Sink sink ^String s]
  (.writeUtf8 sink s))

(defn format-out [^Sink sink response]
  (let [content (-> response
                    (dissoc :sparkling/headers)
                    (generate-string *json-options*))
        headers (assoc (:sparkling/headers response)
                       :Content-Length (count content))]
    (doseq [[header value] headers]
      (doto sink
        (write-str (name header))
        (write-str ": ")
        (write-str (str value))
        (write-str "\r\n")))

    (doto sink
      (write-str "\r\n")
      (write-str content)
      (.flush))))
