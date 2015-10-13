(ns hugsql.core
  (:require [hugsql.parser :as parser]
            [hugsql.parameters :as parameters]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(defn parsed-defs-from-file
  "Given a hugsql SQL file, parse it,
   and return the defs."
  [file]
  (parser/parse
    (slurp
      (condp isa? file
        java.io.File file ; already file
        java.net.URL file ; already resource
        (io/resource file) ; assume resource
        ))))

(defn- jdbc-fn-for
  [query-type]
  `jdbc/query)

(defn prepare-sql
  "Takes an sql template (from hugsql parser)
   and the runtime-provided param data 
   and creates a vector of [\"sql\" val1 val2]
   suitable for jdbc query/execute.

   The :sql vector (sql-template) has interwoven
   sql strings and hashmap parameters.  We directly
   apply parameters to non-prepared parameter types such
   as identifiers and keywords.  For value parameter types,
   we replace use the jdbc prepared statement syntax of a
   '?' to placehold for the value."
  [sql-template param-data options]
  (let [applied (mapv #(if (string? %)
                         [%]
                         (parameters/apply-param % param-data options))
                  sql-template)
        sql    (string/join "" (map first applied))
        params (remove nil? (map second applied))]
    (apply vector sql params)))

(def default-options
  {:quote-identifiers :off})

(defmacro def-sql-str-fns
  "Given a hugsql SQL file, define the <query-name>-sql 
   functions that return the resulting SQL strings"
  ([file] (def-sql-str-fns &form &env file {}))
  ([file options]
   (doseq [d (parsed-defs-from-file file)]
     (let [nm (symbol (str (first (:name (:hdr d))) "-sql"))
           dc (str (first (:doc (:hdr d))) " (sql)")
           sq (:sql d)
           op (merge default-options options)]
       (eval ;; FIXME: get rid of eval
         `(defn ~nm
            ~dc
            ([~'db] (~nm ~'db {} {}))
            ([~'db ~'param-data] (~nm ~'db ~'param-data {}))
            ([~'db ~'param-data ~'options]
             (prepare-sql ~sq ~'param-data (merge ~op ~'options)))))))))

(defmacro def-sql-fns
  "Given a hugsql SQL file, define the database 
   query/execute functions"
  ([file] (def-sql-fns &form &env file {}))
  ([file options]
   (doseq [d (parsed-defs-from-file file)]
     (let [nm (symbol (first (:name (:hdr d))))
           dc (first (:doc (:hdr d)))
           sq (:sql d)
           op (merge default-options options)]
       (eval ;; FIXME: get rid of eval
         `(defn ~nm
            ~dc
            ([~'db] (~nm ~'db {} {}))
            ([~'db ~'param-data] (~nm ~'db ~'param-data {}))
            ([~'db ~'param-data ~'options]
             (prepare-sql ~sq ~'param-data (merge ~op ~'options)))))))))



