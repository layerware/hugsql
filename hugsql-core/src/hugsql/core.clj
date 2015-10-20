(ns hugsql.core
  (:require [hugsql.parser :as parser]
            [hugsql.parameters :as parameters]
            [hugsql.adapter :as adapter]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def ^:private ^:dynamic *adapter* (atom nil))

(defn set-adapter!
  "Set a global adapter."
  [the-adapter]
  (reset! *adapter* the-adapter))

(defn get-adapter
  []
  ;; nothing set yet? set default adapter
  ;; DEV NOTE: I don't really like dynamically eval'ing
  ;; and require'ing here, but I do prefer to have:
  ;; 1) an easy-path/just-works default adapter
  ;; 2) *no* hard-coded default dependency
  ;;    if someone wants to use another adapter
  ;; 3) *no* requirement for an adapter at all unless
  ;;    def-db-fns is used
  (when (nil? @*adapter*)
    (eval
      '(do (clojure.core/require '[hugsql.clojure-java-jdbc-adapter
                      :refer [hugsql-clojure-java-jdbc-adapter]])
           (hugsql.core/set-adapter! (hugsql-clojure-java-jdbc-adapter)))))
  @*adapter*)

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

(def default-options {:quoting :off})
(def default-command :query)
(def default-result :many)

(defn command-sym
  [hdr]
  (keyword
    (or (second (:name hdr))
      (:command hdr)
      default-command)))

(defn result-sym
  [hdr]
  (keyword
    (or (second (next (:name hdr)))
      (:result hdr)
      default-result)))

(defmulti hugsql-command-fn identity)
(defmethod hugsql-command-fn :! [sym] 'hugsql.adapter/execute)
(defmethod hugsql-command-fn :execute [sym] 'hugsql.adapter/execute)
(defmethod hugsql-command-fn :? [sym] 'hugsql.adapter/query)
(defmethod hugsql-command-fn :query [sym] 'hugsql.adapter/query)
(defmethod hugsql-command-fn :default [sym] 'hugsql.adapter/query)

(defmulti hugsql-result-fn identity)
(defmethod hugsql-result-fn :1 [sym] 'hugsql.adapter/result-one)
(defmethod hugsql-result-fn :one [sym] 'hugsql.adapter/result-one)
(defmethod hugsql-result-fn :* [sym] 'hugsql.adapter/result-many)
(defmethod hugsql-result-fn :many [sym] 'hugsql.adapter/result-many)
(defmethod hugsql-result-fn :n [sym] 'hugsql.adapter/result-affected)
(defmethod hugsql-result-fn :affected [sym] 'hugsql.adapter/result-affected)
(defmethod hugsql-result-fn :default [sym] 'hugsql.adapter/result-many)

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
           opt (merge default-options options)
           cmd (hugsql-command-fn (command-sym hdr))
           res (hugsql-result-fn (result-sym hdr))
           adp (or (:adapter opt) (get-adapter))]
       (eval ;; FIXME: get rid of eval
         `(defn ~nam
            ~doc
            ([~'db] (~nam ~'db {} {}))
            ([~'db ~'param-data] (~nam ~'db ~'param-data {}))
            ([~'db ~'param-data ~'options]
             (~cmd ~adp ~'db
               (prepare-sql ~sql ~'param-data (merge ~opt ~'options))
               (merge ~opt ~'options)))))))))



