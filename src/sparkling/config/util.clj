(ns sparkling.config.util)

(defn add-source-paths [project-config paths]
  (update project-config :source-paths
          (fnil into #{})
          paths))
