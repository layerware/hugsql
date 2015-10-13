(ns hugsql.parameters)

(defprotocol ValueParam
  "Protocol to convert Clojure value to SQL value"
  (value-param [param value options]))

(defprotocol ValueParamList
  "Protocol to convert a collection of Clojure
   values to SQL values"
  (value-param-list [param coll options]))

(defprotocol IdentifierParam
  "Protocol to convert a Clojure value to SQL identifier"
  (identifier-param [param identifier options]))

(defprotocol IdentifierParamList
  "Protocol to convert a collection of Clojure
   values to SQL identifiers"
  (identifier-param-list [param coll options]))

(defprotocol SQLParam
  "Protocol to convert a Clojure value to raw SQL"
  (sql-param [param value options]))

(extend-type Object
  ValueParam
  (value-param [param value options]
    ["?" value])

  IdentifierParam
  (identifier-param [param value options]
    [(str "\"" value "\"")]))

(def ^:private ^:dynamic *param-types*
  (atom {:v value-param
         :value value-param
         :v* value-param-list
         :value* value-param-list
         :i identifier-param
         :identifier identifier-param
         :i* identifier-param-list
         :identifier* identifier-param-list
         :sql sql-param}))

(defn param-types [] (deref *param-types*))

(defn register-param
  "Register a parameter type with a keyword
   parameter type and a function to apply 
   the parameter at runtime"
  [param-type param-fn]
  (swap! *param-types* assoc param-type param-fn))

(defn apply-param
  "Given a param hashmap and a param data hashmap,
   return the vector pair [\"sql string ?\" [val1 val2]]"
  [param data options]
  (((:type param) (param-types)) param ((:name param) data) options))


