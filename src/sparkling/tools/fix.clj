(ns sparkling.tools.fix
  (:require [sparkling.tools.fixers.model :refer [declared-fixers]]

            ; import all fixers:
            [sparkling.tools.fixers.duplicate-refer]
            [sparkling.tools.fixers.missing-var]))

(defn parse-error [error]
  (let [msg (if (string? error)
              error
              (ex-message error))]
    (->> @declared-fixers

         ; flatten to pairs of [kind, regex]
         (mapcat (fn [[kind {regexes :matches}]]
                   (map (fn [regex]
                          [kind regex])
                        regexes)))

         (keep (fn [[kind regex]]
                 (when-some [m (re-find regex msg)]
                   {:kind kind
                    :args (next m)})))

         first)))

(defn extract [context error]
  (if-let [{:keys [kind args]} (parse-error error)]
    (if-let [fixer (get-in @declared-fixers [kind :fixer])]
      (apply fixer context args)

      (throw (IllegalStateException.
               (str "Could not find fixer for kind " kind))))

    (println "No fixer found for " (pr-str error))))
