(ns hugsql.parameters
  (:require [clojure.string :as string]))

(defprotocol ValueParam
  "Protocol to convert Clojure value to SQL value"
  (value-param [param data options]))

(defprotocol ValueParamList
  "Protocol to convert a collection of Clojure
   values to SQL values"
  (value-param-list [param data options]))

(defprotocol IdentifierParam
  "Protocol to convert a Clojure value to SQL identifier"
  (identifier-param [param data options]))

(defprotocol IdentifierParamList
  "Protocol to convert a collection of Clojure
   values to SQL identifiers"
  (identifier-param-list [param data options]))

(defprotocol SQLParam
  "Protocol to convert a Clojure value to raw SQL"
  (sql-param [param data options]))

(defn- identifier-param-quote
  [value {:keys [quoting] :as options}]
  (let [parts (string/split value #"\.")
        qtfn  (condp = quoting
                :ansi #(str \" (string/replace % "\"" "\"\"") \")
                :mysql #(str \` (string/replace % "`" "``") \`)
                :mssql #(str \[ (string/replace % "]" "]]") \])
                ;; off:
                identity)]
    (string/join "." (map qtfn parts))))

;; Default Object implementations
(extend-type Object
  ValueParam
  (value-param [param data options]
    ["?" (get data (:name param))])

  ValueParamList
  (value-param-list [param data options]
    (let [coll (get data (:name param))]
      (apply vector
        (string/join "," (repeat (count coll) "?"))
        coll)))

  IdentifierParam
  (identifier-param [param data options]
    [(identifier-param-quote (get data (:name param)) options)])

  IdentifierParamList
  (identifier-param-list [param data options]
    (let [coll (get data (:name param))]
      [(string/join ", "
         (map #(identifier-param-quote % options) coll))]))

  SQLParam
  (sql-param [param data options]
    [(get data (:name param))]))

(defmulti apply-hugsql-param
  "Implementations of this multimethod apply a hugsql parameter
   for a specified parameter type.  For example:

   (defmethod apply-hugsql-param :value
     [param data options]
     (value-param param data options)

   - :value keyword is the parameter type to match on.
   - param is the parameter map as parsed from SQL
     (e.g., {:type :value :name \"id\"} )
   - data is the run-time parameter map data to be applied
     (e.g., {:id 42} )
   - options contain hugsql options (see hugsql.core/def-sql-fns)

   Implementations must return a vector containing any resulting SQL
   in the first position and any values in the remaining positions.
   (e.g., [\"?\" 42])"
  (fn [param data options] (:type param)))

(defmethod apply-hugsql-param :v  [param data options] (value-param param data options))
(defmethod apply-hugsql-param :value [param data options] (value-param param data options))
(defmethod apply-hugsql-param :v*  [param data options] (value-param-list param data options))
(defmethod apply-hugsql-param :value* [param data options] (value-param-list param data options))
(defmethod apply-hugsql-param :i [param data options] (identifier-param param data options))
(defmethod apply-hugsql-param :identifier [param data options] (identifier-param param data options))
(defmethod apply-hugsql-param :i* [param data options] (identifier-param-list param data options))
(defmethod apply-hugsql-param :identifier* [param data options] (identifier-param-list param data options))
(defmethod apply-hugsql-param :sql [param data options] (sql-param param data options))
