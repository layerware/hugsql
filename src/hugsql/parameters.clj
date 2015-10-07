(ns hugsql.parameters)

(defprotocol ValueParam
  "Protocol to convert Clojure value to SQL value"
  (value-param [this value]))

(defprotocol ValueParamList
  "Protocol to convert a collection of Clojure
   values to SQL values"
  (value-param-list [this coll]))

(defprotocol IdentifierParam
  "Protocol to convert a Clojure value to SQL identifier"
  (identifier-param [this identifier]))

(defprotocol IdentifierParamList
  "Protocol to convert a collection of Clojure
   values to SQL identifiers"
  (identifier-param-list [this coll]))

(defprotocol SQLParam
  "Protocol to convert a Clojure value to raw SQL"
  (sql-param [this value]))


(defn apply-param
  "Given a param hashmap and a param data hashmap,
   return the vector pair [\"sql string ?\" [val1 val2]]"
  [param data]
  )


