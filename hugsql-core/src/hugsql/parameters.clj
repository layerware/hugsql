(ns hugsql.parameters
  (:require [clojure.string :as string]))

(defprotocol ValueParam
  "Protocol to convert Clojure value to SQL value"
  (value-param [param data options]))

(defprotocol ValueParamList
  "Protocol to convert a collection of Clojure
   values to SQL values. Similar to a TupleParam,
   but a ValueParamList does NOT enclose its values
   in parentheses.  Generally ValueParamList values
   are of the same type."
  (value-param-list [param data options]))

(defprotocol TupleParam
  "Protocol to convert a collection of Clojure
   values to an SQL tuple. Similar to a ValueParamList,
   but a TupleParam encloses its values in parentheses."
  (tuple-param [param data options]))

(defprotocol TupleParamList
  "Protocol to convert a collection of collections of
   Clojure values to an SQL list of tuples.  This is
   used specifically for multiple-record SQL inserts."
  (tuple-param-list [param data options]))

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

(defprotocol SQLVecParam
  "Protocol to splice in an sqlvec (or snippet)"
  (sqlvec-param [param data options]))

(defprotocol SQLVecParamList
  "Protocol to splice in a collection of sqlvecs (or snippets)"
  (sqlvec-param-list [param data options]))


(defn identifier-param-quote
  "Quote the identifier value based on options."
  [value {:keys [quoting no-dot-split] :as options}]
  (let [parts (if no-dot-split [value] (string/split value #"\."))
        qtfn  (condp = quoting
                :ansi #(str \" (string/replace % "\"" "\"\"") \")
                :mysql #(str \` (string/replace % "`" "``") \`)
                :mssql #(str \[ (string/replace % "]" "]]") \])
                ;; off:
                identity)]
    (string/join "." (map qtfn parts))))

(defn deep-get-vec
  "Takes a param :name and returns a vector
   suitable for get-in lookups where the
   param :name starts with the form:
     :employees.0.id
   Names must be keyword keys in hashmaps in
   param data.
   Numbers must be vector indexes in vectors
   in param data."
  [nam]
  (if (namespace nam)
    [nam] ; deep get not supported for namespaced keywords
    (mapv
     (fn [x] (if (re-find #"^\d+$" x) (Long. ^String x) (keyword x)))
     (string/split (name nam) #"\."))))

;; Default Object implementations
(extend-type Object
  ValueParam
  (value-param [param data options]
    ["?" (get-in data (deep-get-vec (:name param)))])

  ValueParamList
  (value-param-list [param data options]
    (let [coll (get-in data (deep-get-vec (:name param)))]
      (apply vector
        (string/join "," (repeat (count coll) "?"))
        coll)))

  TupleParam
  (tuple-param [param data options]
    (let [vpl (value-param-list param data options)]
      (apply vector (str "(" (first vpl) ")") (rest vpl))))

  TupleParamList
  (tuple-param-list [param data options]
    (let [tuples (map (juxt first rest)
                      (map #(tuple-param {:name :x} {:x %} options)
                           (get-in data (deep-get-vec (:name param)))))
          sql (string/join "," (map first tuples))
          values (apply concat (apply concat (map rest tuples)))]
      (apply vector sql values)))

  IdentifierParam
  (identifier-param [param data options]
    (let [i (get-in data (deep-get-vec (:name param)))
          coll? (instance? clojure.lang.IPersistentCollection i)
          i (if coll? (flatten (into [] i)) i)]
      (if coll?
        [(str (identifier-param-quote (first i) options)
              " as "
              (identifier-param-quote (second i)
                                      (merge options {:no-dot-split true})))]
        [(identifier-param-quote i options)])))

  IdentifierParamList
  (identifier-param-list [param data options]
    [(string/join
      ", "
      (map
       #(if (vector? %)
          (str (identifier-param-quote (first %) options)
               " as "
               (identifier-param-quote (second %)
                                       (merge options {:no-dot-split true})))
          (identifier-param-quote % options))
       (into [] (get-in data (deep-get-vec (:name param))))))])

  SQLParam
  (sql-param [param data options]
    [(get-in data (deep-get-vec (:name param)))])

  SQLVecParam
  (sqlvec-param [param data options]
    (get-in data (deep-get-vec (:name param))))

  SQLVecParamList
  (sqlvec-param-list [param data options]
    (reduce
      #(apply vector
         (string/join " " [(first %1) (first %2)])
         (concat (rest %1) (rest %2)))
      (get-in data (deep-get-vec (:name param))))))

(defmulti apply-hugsql-param
  "Implementations of this multimethod apply a hugsql parameter
   for a specified parameter type.  For example:

   (defmethod apply-hugsql-param :value
     [param data options]
     (value-param param data options)

   - the :value keyword is the parameter type to match on.
   - param is the parameter map as parsed from SQL
     (e.g., {:type :value :name \"id\"} )
   - data is the run-time parameter map data to be applied
     (e.g., {:id 42} )
   - options contain hugsql options (see hugsql.core/def-sqlvec-fns)

   Implementations must return a vector containing any resulting SQL
   in the first position and any values in the remaining positions.
   (e.g., [\"?\" 42])"
  (fn [param data options] (:type param)))

(defmethod apply-hugsql-param :v  [param data options] (value-param param data options))
(defmethod apply-hugsql-param :value [param data options] (value-param param data options))
(defmethod apply-hugsql-param :v*  [param data options] (value-param-list param data options))
(defmethod apply-hugsql-param :value* [param data options] (value-param-list param data options))
(defmethod apply-hugsql-param :t [param data options] (tuple-param param data options))
(defmethod apply-hugsql-param :tuple [param data options] (tuple-param param data options))
(defmethod apply-hugsql-param :t* [param data options] (tuple-param-list param data options))
(defmethod apply-hugsql-param :tuple* [param data options] (tuple-param-list param data options))
(defmethod apply-hugsql-param :i [param data options] (identifier-param param data options))
(defmethod apply-hugsql-param :identifier [param data options] (identifier-param param data options))
(defmethod apply-hugsql-param :i* [param data options] (identifier-param-list param data options))
(defmethod apply-hugsql-param :identifier* [param data options] (identifier-param-list param data options))
(defmethod apply-hugsql-param :sql [param data options] (sql-param param data options))
(defmethod apply-hugsql-param :sqlvec [param data options] (sqlvec-param param data options))
(defmethod apply-hugsql-param :sqlvec* [param data options] (sqlvec-param-list param data options))
(defmethod apply-hugsql-param :snip [param data options] (sqlvec-param param data options))
(defmethod apply-hugsql-param :snip* [param data options] (sqlvec-param-list param data options))
