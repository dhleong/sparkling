(ns sparkling.util.promise
  (:require [promesa.core :as p]))

(defn fallback
  "Given a variable number of fns, each of which is expected to
   return a Promise, returns a fn that calls each, in order, falling
   back to the next if the promise returned from the one before rejected."
  [& fns]
  (if (= 1 (count fns))
    (first fns)

    (fn fallback-fn [& args]
      (reduce
        (fn [p next-fn]
          (p/catch p (fn [e]
                       (println "nrepl WARN: " e)
                       (apply next-fn args))))

        (apply (first fns) args)
        (next fns)))))

(defn with-timing [label p]
  (let [start (. System (nanoTime))]
    (p/then
      p
      (fn [result]
        (prn (str "[" label "] Elapsed time: "
                  (/ (double (- (. System (nanoTime)) start)) 1000000.0)
                  " msecs"))
        result))))
