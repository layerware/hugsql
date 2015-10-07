(ns hugsql.core
  (:require [hugsql.parser :as parser]
            [hugsql.parameters :as parameters]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]))

(defn- parsed-defs-from-file
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

(defn- sql-or-apply-param
  "The :sql vector returned from a hugsql
   parse has sql strings and hashmap parameters
   interwoven.  This function returns a 
   vector of [param-applied-sql value(s)]"
  [param data]
  (if (string? param)
    [param nil]
    (parameters/apply-param param data)))

(defn- build-sql
  "Takes an sql template (from hugsql parser)
   and the runtime-provided param data 
   and creates a vector of [\"sql\" val1 val2]
   suitable for jdbc query/execute."
  [sql-template param-data]
  (reduce
    #(sql-or-apply-param % param-data)
    sql-template))

(defmacro def-sql-string-fns
  "Given a hugsql SQL file, define the <query-name>-sql 
   functions that return the resulting SQL strings"
  [file]
  (doseq [d (parsed-defs-from-file file)]
    (let [hdr (:hdr d)
          jdbc-fn (jdbc-fn-for (:query-type hdr))
          db (gensym "db_")
          pd (gensym "param_data_")]
      `(defn ~(:name hdr)
         ~(:doc hdr)
         [~db ~pd]
         (build-sql ~(:sql d) ~pd)))
    ))

(defmacro def-sql-fns
  "Given a hugsql SQL file, define the database 
   query/execute functions"
  [file]
  (let [file-str (slurp file)
        parsed-defs (parser/parse file-str)]))



