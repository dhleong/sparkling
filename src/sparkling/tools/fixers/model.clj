(ns sparkling.tools.fixers.model)

(defonce declared-fixers (atom {}))

(defmacro def-fixer
  [kind metadata params & body]
  (let [fixer-name (symbol (str "fix-" (name kind)))]
    `(do
       (defn ~fixer-name
         ~metadata
         ~params
         ~@body)

       (swap! declared-fixers assoc ~kind ~(assoc metadata
                                                  :fixer fixer-name)))))
