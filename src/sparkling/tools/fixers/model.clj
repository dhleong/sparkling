(ns sparkling.tools.fixers.model
  (:require [promesa.core :as p]
            [sparkling.tools.fixers.spec :as spec]
            [sparkling.spec.util :refer [validate]]))

(defonce declared-fixers (atom {}))

(defmacro def-fixer
  "Declare a fixer function, using defn-like syntax.
   `kind`: a keyword describing the fix type
   `metadata`: a map, including:
    - :matches a vector of regexes
   `params`: a params vector (like with defn) accepting
             a context map and any matched regex groups.

   The result of the fixer function must either be nil, if we couldn't fix the
   issue, a single ::spec/fix, or a collection of ::spec/fix. The result of
   invoking the fix will be coerced into a collection.
   "
  [kind metadata params & body]
  (let [fixer-name (symbol (str "fix-" (name kind)))]
    `(do
       (defn ~fixer-name
         ~metadata
         ~params
         (p/let [fix# (do ~@body)]
           (when fix#
             (validate ::spec/fix-result fix#)
             (if (map? fix#)
               [fix#]
               fix#))))

       (swap! declared-fixers assoc ~kind ~(assoc metadata
                                                  :fixer fixer-name)))))
