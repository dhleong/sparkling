(ns sparkling.handlers.core)

(def handlers (atom {}))

(defn define [method handler]
  (swap! handlers assoc method handler))

(defmacro defhandler [k & fdecl]
  (let [fn-name (symbol (str "handle-"
                             (when-let [n (namespace k)]
                               (str n "-"))
                             (name k)))]
    `(do
       (defn ~fn-name
         ~@fdecl)
       (#'sparkling.handlers.core/define
         ~k
         ~fn-name))))
