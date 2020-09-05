(ns sparkling.static
  (:require [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [promesa.core :as p]
            [systemic.core :refer [defsys]]
            [sparkling.config :refer [*project-config*]]
            [sparkling.static.classpath :refer [classpath-at]]
            [sparkling.static.kondo :as kondo]
            [sparkling.util.promise :as promise]))

(defn- cache-up-to-date? [{:keys [cp kondo]} cached]
  (and (= (:cp cached) cp)
       (= (:kondo cached) kondo)))

(defn- ->cache [{:keys [cp kondo]} data]
  {:cp cp
   :kondo kondo
   :data data})

(defn- load-kondo-classpath [config]
  ; TODO is it worth it to gzip this file?
  (p/let [cache-file (io/file (:root-path config)
                              ".sparkling/.cache/classpath.transit.json")
          cp (classpath-at (:root-path config))
          kondo-version (kondo/version)

          cache-context {:cp cp
                         :kondo kondo-version}

          cached (try (-> cache-file
                          (io/input-stream)
                          (transit/reader :json)
                          (transit/read))
                      (catch Exception e
                        (println "Unable to load cached cp data: " e)
                        nil))]
    (println "Fetching kondo classpath for" config ": " cp)

    (if (cache-up-to-date? cache-context
                           cached)
      ; easy, fast case
      (do
        (println "Use cached classpath data")
        (:data cached))

      (p/let [_ (println "updating classpath analysis cache for: " config)

              fresh (promise/with-timing (str "analyze kondo classpath for " config)
                      (kondo/analyze-path (:root-path config) cp))]

        ; cache analysis
        (println "Caching classpath analysis to: " cache-file)
        (io/make-parents cache-file)
        (-> cache-file
            (io/output-stream)
            (transit/writer :json)
            (transit/write (->cache cache-context fresh)))

        fresh))))

(defsys *kondo-classpath*
  "A promise that resolves to a clj-kondo-powered analysis
   of the project classpath"
  :deps [*project-config*]
  :start (p/let [config *project-config*]
           (println "Start loading kondo classpath for " config)
           (promise/with-timing (str "load kondo classpath for " config)
             (load-kondo-classpath config))))


; ======= Project path analysis ===========================

(defsys *kondo-project-path*
  "A promise that resolves to a clj-kondo-powered analysis
   of the project root"
  :deps [*project-config*]
  :start (promise/with-timing "analyze project path"
           (p/let [config *project-config*]
             (kondo/analyze-path (:root-path config) (:root-path config)))))
