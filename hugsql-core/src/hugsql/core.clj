(ns hugsql.core
  (:require [hugsql.parser :as parser]
            [hugsql.parameters :as parameters]
            [hugsql.adapter :as adapter]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def ^:private adapter nil)

(defn set-adapter!
  "Set a global adapter."
  [the-adapter]
  (alter-var-root #'adapter (constantly the-adapter)))

(defn ^:no-doc get-adapter
  []
  ;; nothing set yet? set default adapter
  ;; DEV NOTE: I don't really like dynamically eval'ing
  ;; and require'ing here, but I do prefer to have:
  ;; 1) an easy-path/just-works default adapter
  ;; 2) *no* hard-coded default dependency
  ;;    if someone wants to use another adapter
  ;; 3) *no* requirement for an adapter at all unless
  ;;    def-db-fns is used and calls this function
  (when (nil? adapter)
    (eval
      '(do (clojure.core/require '[hugsql.adapter.clojure-java-jdbc :as adp])
           (hugsql.core/set-adapter! (adp/hugsql-adapter-clojure-java-jdbc)))))
  adapter)

(defn ^:no-doc parsed-defs-from-file
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

(defn ^:no-doc prepare-sql
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

(def ^:private default-options {:quoting :off})

(defn- str->key
  [str]
  (keyword (string/replace-first str #":" "")))

(defn- command-sym
  [hdr]
  (or
    ;;                ↓ short-hand command position
    ;; -- :name my-fn :? :1
    (when-let [c (second (:name hdr))] (str->key c))
    ;; -- :command :?
    (when-let [c (first (:command hdr))] (str->key c))
    ;; default
    :query))

(defn- result-sym
  [hdr]
  (keyword
    (or
      ;;                   ↓ short-hand result position
      ;; -- :name my-fn :? :1
      (when-let [r (second (next (:name hdr)))] (str->key r))
      ;; -- :result :1
      (when-let [r (first (:result hdr))] (str->key r))
      ;; default
      :raw)))

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
(defmethod hugsql-result-fn :raw [sym] 'hugsql.adapter/result-raw)
(defmethod hugsql-result-fn :default [sym] 'hugsql.adapter/result-raw)

(defmacro def-sqlvec-fns
  "Given a hugsql SQL file, define the <query-name>-sqlvec
   functions that return the vector of SQL and parameters.
   (e.g., [\"select * from test where id = ?\" 42])

   The likely use case for the sqlvec format is for
   clojure.java.jdbc/query,execute and
   clojure.jdbc/fetch,execute -- both libraries use this
   sqlvec convention.

   Replacement of value parameters is deferred to the
   underlying library."
  ([file] (def-sqlvec-fns &form &env file {}))
  ([file options]
   `(do
      ~@(for [d (parsed-defs-from-file file)]
          (let [hdr (:hdr d)
                nam (symbol (str (first (:name hdr)) "-sqlvec"))
                doc (str (first (:doc hdr)) " (sqlvec)")
                sql (:sql d)
                opt (merge default-options options)]
            `(defn ~nam
               ~doc
               ([] (~nam {} {}))
               ([~'param-data] (~nam ~'param-data {}))
               ([~'param-data ~'options]
                (prepare-sql ~sql ~'param-data (merge ~opt ~'options)))))))))

(defmacro def-db-fns
  "Given a hugsql SQL file, define the database
   query/execute functions"
  ([file] (def-db-fns &form &env file {}))
  ([file options]
   `(do
      ~@(for [d (parsed-defs-from-file file)]
          (let [hdr (:hdr d)
                nam (symbol (first (:name hdr)))
                doc (or (first (:doc hdr)) "")
                sql (:sql d)
                opt (merge default-options options)
                cmd (hugsql-command-fn (command-sym hdr))
                res (hugsql-result-fn (result-sym hdr))
                adp (or (:adapter opt) (get-adapter))]
            `(defn ~nam
               ~doc
               ([~'db] (~nam ~'db {} {}))
               ([~'db ~'param-data] (~nam ~'db ~'param-data {}))
               ([~'db ~'param-data ~'options]
                (let [o# (merge ~opt ~'options)]
                  (~res ~adp
                        (~cmd ~adp ~'db
                              (prepare-sql ~sql ~'param-data o#)
                              o#)
                        o#)))))))))
