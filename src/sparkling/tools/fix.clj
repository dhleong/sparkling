(ns sparkling.tools.fix
  (:require [sparkling.tools.fixers.model :refer [declared-fixers]]

            ; import all fixers:
            [sparkling.tools.fixers.duplicate-refer]))

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

         (map (fn [[kind regex]]
                (when-some [m (re-find regex msg)]
                  {:kind kind
                   :args (next m)})))

         first)))

(defn apply-fix [error]
  (when-let [{:keys [kind args]} (parse-error error)]
    (if-let [fixer (get-in @declared-fixers [kind :fixer])]
      (apply fixer args)

      (throw (IllegalStateException.
               (str "Could not find fixer for kind " kind))))))
