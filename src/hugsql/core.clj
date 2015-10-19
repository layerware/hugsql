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
  (let [applied (mapv
                  #(if (string? %)
                     [%]
                     (parameters/apply-hugsql-param % param-data options))
                  sql-template)
        sql    (string/join "" (map first applied))
        params (flatten (remove empty? (map rest applied)))]
    (apply vector sql params)))

(def default-options {:quoting :off
                      :command-fns {:execute 'jdbc/execute
                                    :query 'jdbc/query}})
(def default-command :query)
(def default-result :many)

(defn command-fn
  [hdr options]
  (get (:command-fns options)
    (symbol
      (or (second (:name hdr))
        (:command hdr)
        default-command))))

(defn result-type
  [hdr]
  (symbol
    (or (second (next (:name hdr)))
      (:result hdr)
      default-result)))

(defn command-fn
  [command]
  (condp = command
    :query 'jdbc/query
    :? 'jdbc/query
    :execute 'jdbc/execute
    :! 'jdbc/execute))

(defn result-fn
  [result]
  (condp = result
    :))

(defmacro def-sql-fns
  "Given a hugsql SQL file, define the <query-name>-sql 
   functions that returns the vector of prepared SQL and 
   parameters. (e.g., [\"select * from test where id = ?\" 42])"
  ([file] (def-sql-fns &form &env file {}))
  ([file options]
   (doseq [d (parsed-defs-from-file file)]
     (let [hdr (:hdr d)
           nam (symbol (str (first (:name hdr)) "-sql"))
           doc (str (first (:doc hdr)) " (sql)")
           sql (:sql d)
           opt (merge default-options options)]
       (eval ;; FIXME: get rid of eval
         `(defn ~nam
            ~doc
            ([~'db] (~nam ~'db {} {}))
            ([~'db ~'param-data] (~nam ~'db ~'param-data {}))
            ([~'db ~'param-data ~'options]
             (prepare-sql ~sql ~'param-data (merge ~opt ~'options)))))))))

(defmacro def-db-fns
  "Given a hugsql SQL file, define the database 
   query/execute functions"
  ([file] (def-db-fns &form &env file {}))
  ([file options]
   (doseq [d (parsed-defs-from-file file)]
     (let [hdr (:hdr d)
           nam (symbol (first (:name hdr)))
           doc (first (:doc hdr))
           sql (:sql d)
           cmd (command-type hdr)
           res (result-type hdr)
           opt (merge default-options options)]
       (eval ;; FIXME: get rid of eval
         `(defn ~nam
            ~doc
            ([~'db] (~nam ~'db {} {}))
            ([~'db ~'param-data] (~nam ~'db ~'param-data {}))
            ([~'db ~'param-data ~'options]
             (prepare-sql ~sql ~'param-data (merge ~opt ~'options)))))))))



