(ns hugsql.core
  (:require [hugsql.parser :as parser]
            [hugsql.parameters :as parameters]
            [hugsql.adapter :as adapter]
            [hugsql.expr-run]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.tools.reader.edn :as edn]))

(def ^:no-doc adapter nil)

(defn set-adapter!
  "Set a global adapter."
  [the-adapter]
  (alter-var-root #'adapter (constantly the-adapter)))

(defn ^:no-doc get-adapter
  "Get an adapter.  Sets default
   hugsql.adapter.clojure-java-jdbc
   adapter if no adapter is set."
  []
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

(defn ^:no-doc parsed-defs-from-string
  "Given a hugsql SQL string,
   parse it, and return the defs."
  [sql]
  (parser/parse sql))

(defn ^:no-doc parsed-defs-from-file
  "Given a hugsql SQL file in the classpath,
   a resource, or a java.io.File, parse it, and return the defs."
  [file]
  (parser/parse
   (slurp
    (condp instance? file
      java.io.File file ; already file
      java.net.URL file ; already resource
      ;; assume resource path (on classpath)
      (if-let [f (io/resource file)]
        f
        (throw (ex-info (str "Can not read file: " file) {})))))))

(defn ^:no-doc validate-parsed-def!
  "Ensure SQL required headers are provided
   and throw an exception if not."
  [pdef]
  (let [hdr (:hdr pdef)]
    (when-not (or (:name hdr) (:name- hdr) (:snip hdr) (:snip- hdr))
      (throw (ex-info
              (str "Missing HugSQL Header of :name, :name-, :snip, or :snip-\n"
                   "Found headers include: " (pr-str (vec (keys hdr))) "\n"
                   "SQL: " (pr-str (:sql pdef))) {})))))

(defn ^:no-doc validate-parameters!
  "Ensure SQL template parameters match provided param-data,
   and throw an exception if mismatch."
  [sql-template param-data]
  (let [not-found (Object.)]
    (doseq [k (map :name (filter map? sql-template))]
      (when-not
          (not-any?
           #(= not-found %)
           (map #(get-in param-data % not-found)
                (rest (reductions
                       (fn [r x] (conj r x))
                       []
                       (parameters/deep-get-vec k)))))
        (throw (ex-info
                (str "Parameter Mismatch: "
                     k " parameter data not found.") {}))))))

(defn ^:no-doc expr-name
  [expr]
  (str "expr-" (hash (pr-str expr))))

(defn ^:no-doc def-expr
  ([expr] (def-expr expr nil))
  ([expr require-str]
   (let [nam (expr-name expr)
         ;; tag expressions vs others
         ;; and collect interspersed items together into a vector
         tag (reduce
              (fn [r c]
                (if (vector? c)
                  (conj r {:expr c})
                  (if-let [o (:other (last r))]
                    (conj (vec (butlast r)) (assoc (last r) :other (conj o c)))
                    (conj r {:other [c]}))))
              []
              expr)
         clj  (str
              "(ns hugsql.expr-run\n"
              (when-not (string/blank? require-str)
                (str "(:require " require-str ")"))
              ")\n"
              "(defn " nam " [params options] "
              (string/join
               " "
               (filter
                #(not (= % :cont))
                (map (fn [x]
                       (if (:expr x)
                         (first (:expr x))
                         (pr-str (:other x)))) tag)))
              ")")
         expr-fn #(ns-resolve 'hugsql.expr-run (symbol nam))]
     (when (nil? (expr-fn)) (load-string clj)))))

(defn ^:no-doc compile-exprs
  "Compile (def) all expressions in a parsed def"
  [pdef]
  (let [require-str (string/join " " (:require (:hdr pdef)))]
    (doseq [expr (filter vector? (:sql pdef))]
      (def-expr expr require-str))))

(defn ^:no-doc spacer
  "Check for string s with no leading or
   trailing space and add one if needed."
  [s]
  (let [s (if (Character/isWhitespace ^Character (first s)) s (str " " s))
        s (if (Character/isWhitespace ^Character (last s)) s (str s " "))]
    s))

(defn ^:no-doc run-expr
  "Run expression and return result.
   Example assuming cols is [\"id\"]:

   [[\"(if (seq (:cols params))\" :cont]
     {:type :i* :name :cols}
     [:cont] \"*\" [\")\" :end]]
   to:
   {:type :i* :name :cols}"
  [expr params options]
  (let [expr-fn #(ns-resolve 'hugsql.expr-run (symbol (expr-name expr)))]
    (when (nil? (expr-fn)) (def-expr expr))
    (let [result ((expr-fn) params options)]
      (if (string? result)
        (:sql (first (parser/parse (spacer result) {:no-header true})))
        result))))

(defn ^:no-doc expr-pass
  "Takes an sql template (from hugsql parser) and evaluates the
  Clojure expressions resulting in returning an sql template
  containing only sql strings and hashmap parameters"
  [sql-template params options]
  (loop [curr (first sql-template)
         pile (rest sql-template)
         rsql []
         expr []]
    (if-not curr
      rsql
      (cond
        (or (vector? curr) (seq expr)) ;; expr start OR already in expr
        ;; expr end found, so run
        (if (and (vector? curr) (= :end (last curr)))
          (recur (first pile) (rest pile)
                 (if-let [r (run-expr (conj expr curr) params options)]
                   (vec (concat rsql (if (string? r) (vector r) r)))
                   rsql) [])
          (recur (first pile) (rest pile)
                 rsql (conj expr curr)))

        :else
        (recur (first pile) (rest pile)
               (conj rsql (if (string? curr) (spacer curr) curr)) expr)))))

(defn ^:no-doc prepare-sql
  "Takes an sql template (from hugsql parser) and the runtime-provided
  param data and creates a vector of [\"sql\" val1 val2] suitable for
  jdbc query/execute.

  The :sql vector (sql-template) has interwoven sql strings, vectors
  of Clojure code, and hashmap parameters.  We directly apply
  parameters to non-prepared parameter types such as identifiers and
  keywords.  For value parameter types, we replace use the jdbc
  prepared statement syntax of a '?' to placehold for the value."
  ([sql-template param-data options]
   (let [sql-template (expr-pass sql-template param-data options)
         _ (validate-parameters! sql-template param-data)
         applied (mapv
                  #(if (string? %)
                     [%]
                     (parameters/apply-hugsql-param % param-data options))
                  sql-template)
         sql    (string/join "" (map first applied))
         params (flatten (remove empty? (map rest applied)))]
     (apply vector (string/trim sql) params))))

(def default-sqlvec-options
  {:quoting :off
   :fn-suffix "-sqlvec"})

(def default-db-options {:quoting :off})

(defn- str->key
  [str]
  (keyword (string/replace-first str #":" "")))

(defn ^:no-doc command-sym
  [hdr]
  (or
   ;;                ↓ short-hand command position
   ;; -- :name my-fn :? :1
   (when-let [c (second (:name hdr))] (str->key c))
   ;; -- :command :?
   (when-let [c (first (:command hdr))] (str->key c))
   ;; default
   :query))

(defn ^:no-doc result-sym
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
(defmethod hugsql-command-fn :i! [sym] 'hugsql.adapter/execute)
(defmethod hugsql-command-fn :insert [sym] 'hugsql.adapter/execute)
(defmethod hugsql-command-fn :<! [sym] 'hugsql.adapter/query)
(defmethod hugsql-command-fn :returning-execute [sym] 'hugsql.adapter/query)
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

(defn sqlvec-fn*
  "Given parsed sql and optional options, return an
   anonymous function that returns hugsql sqlvec format"
  ([psql] (sqlvec-fn* psql {}))
  ([psql options]
   (fn y
     ([] (y {} {}))
     ([param-data] (y param-data {}))
     ([param-data opts]
      (prepare-sql psql param-data (merge default-sqlvec-options options opts))))))

(defn sqlvec-fn
  "Given an sql string and optional options, return an
   anonymous function that returns hugsql sqlvec format"
  ([sql] (sqlvec-fn sql {}))
  ([sql options]
   (let [psql (:sql (first (parser/parse sql {:no-header true})))]
     (sqlvec-fn* psql options))))

(def snip-fn "Alias for sqlvec-fn" sqlvec-fn)

(defn sqlvec
  "Given an sql string, optional options, and param data, return an sqlvec"
  ([sql param-data] (sqlvec sql {} param-data))
  ([sql options param-data]
   (let [f (sqlvec-fn sql options)]
     (f param-data))))

(def snip "Alias for sqlvec" sqlvec)

(defn sqlvec-fn-map
  "Hashmap of sqlvec/snip fn from a parsed def
   with the form:
   {:fn-name {:meta {:doc \"doc string\"}
              :fn <anon-db-fn>}"
  [pdef options]
  (let [sql (:sql pdef)
        hdr (:hdr pdef)
        sn- (:snip- hdr) ;; private snippet
        snn (:snip hdr)  ;; public snippet
        nm- (:name- hdr) ;; private name
        nmn (:name hdr)  ;; public name
        nam (symbol
             (str (first (or sn- snn nm- nmn))
                  (when (or nm- nmn) (:fn-suffix (merge default-sqlvec-options options)))))
        doc (str (or (first (:doc hdr)) "") (when (or nm- nmn) " (sqlvec)"))
        mta (if-let [m (:meta hdr)]
              (edn/read-string (string/join " " m)) {})
        met (merge mta
                   {:doc doc}
                   (when (or sn- nm-) {:private true})
                   (when (or sn- snn) {:snip? true}))]
    {(keyword nam) {:meta met
                    :fn (sqlvec-fn* sql options)}}))

(defn intern-sqlvec-fn
  "Intern the sqlvec fn from a parsed def"
  [pdef options]
  (let [fm (sqlvec-fn-map pdef options)
        fk (ffirst fm)]
    (intern *ns*
            (with-meta (symbol (name fk)) (-> fm fk :meta))
            (-> fm fk :fn))))

(defmacro def-sqlvec-fns
  "Given a HugSQL SQL file, define the <name>-sqlvec functions in the
  current namespace.  Returns sqlvec format: a vector of SQL and
  parameter values. (e.g., [\"select * from test where id = ?\" 42])

  Usage:

   (def-sqlvec-fns file options?)

   where:
    - file is a string file path in your classpath,
      a resource object (java.net.URL),
      or a file object (java.io.File)
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :fn-suffix \"-sqlvec\" (default)

   See hugsql.core/def-db-fns for :quoting option details.

   :fn-suffix is appended to the defined function names to
   differentiate them from the functions defined by def-db-fns."
  ([file] (def-sqlvec-fns &form &env file {}))
  ([file options]
   `(doseq [~'pdef (parsed-defs-from-file ~file)]
      (validate-parsed-def! ~'pdef)
      (compile-exprs ~'pdef)
      (intern-sqlvec-fn ~'pdef ~options))))

(defmacro def-sqlvec-fns-from-string
  "Given a HugSQL SQL string, define the <name>-sqlvec functions in the
  current namespace.  Returns sqlvec format: a vector of SQL and
  parameter values. (e.g., [\"select * from test where id = ?\" 42])

  Usage:

   (def-sqlvec-fns-from-string s options?)

   where:
    - s is a HugSQL-flavored sql string
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :fn-suffix \"-sqlvec\" (default)

   See hugsql.core/def-db-fns for :quoting option details.

   :fn-suffix is appended to the defined function names to
   differentiate them from the functions defined by def-db-fns."
  ([s] (def-sqlvec-fns-from-string &form &env s {}))
  ([s options]
   `(doseq [~'pdef (parsed-defs-from-string ~s)]
      (validate-parsed-def! ~'pdef)
      (compile-exprs ~'pdef)
      (intern-sqlvec-fn ~'pdef ~options))))

(defmacro map-of-sqlvec-fns
  "Given a HugSQL SQL file, return a hashmap of database
   functions of the form:

   {:fn1-name {:meta {:doc \"doc string\"}
               :fn <fn1>}
    :fn2-name {:meta {:doc \"doc string\"
                      :private true}
               :fn <fn2>}}

  Usage:

   (map-sqlvec-fns file options?)

   where:
    - file is a string file path in your classpath,
      a resource object (java.net.URL),
      or a file object (java.io.File)
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :fn-suffix \"-sqlvec\" (default)

   See hugsql.core/def-db-fns for :quoting option details.

   :fn-suffix is appended to the defined function names to
   differentiate them from the functions defined by def-db-fns."
  ([file] (map-of-sqlvec-fns &form &env file {}))
  ([file options]
   `(let [~'pdefs (parsed-defs-from-file ~file)]
      (doseq [~'pdef ~'pdefs]
        (validate-parsed-def! ~'pdef)
        (compile-exprs ~'pdef))
      (apply merge
             (map #(sqlvec-fn-map % ~options) ~'pdefs)))))

(defmacro map-of-sqlvec-fns-from-string
  "Given a HugSQL SQL string, return a hashmap of sqlvec
   functions of the form:

   {:fn1-name {:meta {:doc \"doc string\"}
               :fn <fn1>}
    :fn2-name {:meta {:doc \"doc string\"
                      :private true}
               :fn <fn2>}}

  Usage:

   (map-sqlvec-fns-from-string s options?)

   where:
    - s is a HugSQL-flavored sql string
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :fn-suffix \"-sqlvec\" (default)

   See hugsql.core/def-db-fns for :quoting option details.

   :fn-suffix is appended to the defined function names to
   differentiate them from the functions defined by def-db-fns."
  ([s] (map-of-sqlvec-fns-from-string &form &env s {}))
  ([s options]
   `(let [~'pdefs (parsed-defs-from-string ~s)]
      (doseq [~'pdef ~'pdefs]
        (validate-parsed-def! ~'pdef)
        (compile-exprs ~'pdef))
      (apply merge
             (map #(sqlvec-fn-map % ~options) ~'pdefs)))))

(defn db-fn*
  "Given parsed sql and optionally a command, result, and options,
  return an anonymous function that can run hugsql database
  execute/queries and supports hugsql parameter replacement"
  ([psql] (db-fn* psql :default :default {}))
  ([psql command] (db-fn* psql command :default {}))
  ([psql command result] (db-fn* psql command result {}))
  ([psql command result options]
   (fn y
     ([db] (y db {} {}))
     ([db param-data] (y db param-data {}))
     ([db param-data opts & command-opts]
      (let [o (merge default-db-options options opts
                     {:command command :result result})
            o (if (seq command-opts)
                (assoc o :command-options command-opts) o)
            a (or (:adapter o) (get-adapter))]
        (try
          (as-> psql x
            (prepare-sql x param-data o)
            ((resolve (hugsql-command-fn command)) a db x o)
            ((resolve (hugsql-result-fn result)) a x o))
          (catch Exception e
            (adapter/on-exception a e))))))))

(defn db-fn
  "Given an sql string and optionally a command, result, and options,
  return an anonymous function that can run hugsql database
  execute/queries and supports hugsql parameter replacement"
  ([sql] (db-fn sql :default :default {}))
  ([sql command] (db-fn sql command :default {}))
  ([sql command result] (db-fn sql command result {}))
  ([sql command result options]
   (let [psql (:sql (first (parser/parse sql {:no-header true})))]
     (db-fn* psql command result options))))

(defn db-fn-map
  "Hashmap of db fn from a parsed def
   with the form:
   {:fn-name {:meta {:doc \"doc string\"}
              :fn <anon-db-fn>}"
  [pdef options]
  (let [sql (:sql pdef)
        hdr (:hdr pdef)
        pnm (:name- hdr)
        nam (symbol (first (or (:name hdr) pnm)))
        doc (or (first (:doc hdr)) "")
        cmd (command-sym hdr)
        res (result-sym hdr)
        mta (if-let [m (:meta hdr)]
              (edn/read-string (string/join " " m)) {})
        met (merge mta {:doc doc} (when pnm {:private true}))]
    {(keyword nam) {:meta met
                    :fn (db-fn* sql cmd res options)}}))

(defn intern-db-fn
  "Intern the db fn from a parsed def"
  [pdef options]
  (let [fm (db-fn-map pdef options)
        fk (ffirst fm)]
    (intern *ns*
            (with-meta (symbol (name fk)) (-> fm fk :meta))
            (-> fm fk :fn))))

(defn ^:no-doc snippet-pdef?
  [pdef]
  (or (:snip- (:hdr pdef)) (:snip (:hdr pdef))))

(defmacro def-db-fns
  "Given a HugSQL SQL file, define the database
   functions in the current namespace.

   Usage:

   (def-db-fns file options?)

   where:
    - file is a string file path in your classpath,
      a resource object (java.net.URL),
      or a file object (java.io.File)
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :adapter adapter }

   :quoting options for identifiers are:
     :ansi double-quotes: \"identifier\"
     :mysql backticks: `identifier`
     :mssql square brackets: [identifier]
     :off no quoting (default)

   Identifiers containing a period/dot . are split, quoted separately,
   and then rejoined. This supports myschema.mytable conventions.

   :quoting can be overridden as an option in the calls to functions
   created by def-db-fns.

   :adapter specifies the HugSQL adapter to use for all defined
   functions. The default adapter used is
   (hugsql.adapter.clojure-java-jdbc/hugsql-adapter-clojure-java-jdbc)
   when :adapter is not given.

   See also hugsql.core/set-adapter! to set adapter for all def-db-fns
   calls.  Also, :adapter can be specified for individual function
   calls (overriding set-adapter! and the :adapter option here)."
  ([file] (def-db-fns &form &env file {}))
  ([file options]
   `(doseq [~'pdef (parsed-defs-from-file ~file)]
      (validate-parsed-def! ~'pdef)
      (compile-exprs ~'pdef)
      (if (snippet-pdef? ~'pdef)
        (intern-sqlvec-fn ~'pdef ~options)
        (intern-db-fn ~'pdef ~options)))))

(defmacro def-db-fns-from-string
  "Given a HugSQL SQL string, define the database
   functions in the current namespace.

   Usage:

   (def-db-fns-from-string s options?)

   where:
    - s is a string of HugSQL-flavored sql statements
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :adapter adapter }

   See hugsql.core/def-db-fns for :quoting and :adapter details."
  ([s] (def-db-fns-from-string &form &env s {}))
  ([s options]
   `(doseq [~'pdef (parsed-defs-from-string ~s)]
      (validate-parsed-def! ~'pdef)
      (compile-exprs ~'pdef)
      (if (snippet-pdef? ~'pdef)
        (intern-sqlvec-fn ~'pdef ~options)
        (intern-db-fn ~'pdef ~options)))))

(defmacro map-of-db-fns
  "Given a HugSQL SQL file, return a hashmap of database
   functions of the form:

   {:fn1-name {:meta {:doc \"doc string\"}
               :fn <fn1>}
    :fn2-name {:meta {:doc \"doc string\"
                      :private true}
               :fn <fn2>}}

   Usage:

   (map-of-db-fns file options?)

   where:
    - file is a string file path in your classpath,
      a resource object (java.net.URL),
      or a file object (java.io.File)
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :adapter adapter }

   See hugsql.core/def-db-fns for :quoting and :adapter details."
  ([file] (map-of-db-fns &form &env file {}))
  ([file options]
   `(let [~'pdefs (parsed-defs-from-file ~file)]
      (doseq [~'pdef ~'pdefs]
        (validate-parsed-def! ~'pdef)
        (compile-exprs ~'pdef))
      (apply merge
             (map
              #(if (snippet-pdef? %)
                 (sqlvec-fn-map % ~options)
                 (db-fn-map % ~options))
              ~'pdefs)))))

(defmacro map-of-db-fns-from-string
  "Given a HugSQL SQL string, return a hashmap of database
   functions of the form:

   {:fn1-name {:meta {:doc \"doc string\"}
               :fn <fn1>}
    :fn2-name {:meta {:doc \"doc string\"
                      :private true}
               :fn <fn2>}}

   Usage:

   (map-of-db-fns-from-string s options?)

   where:
    - s is a string of HugSQL-flavored sql statements
    - options (optional) hashmap:
      {:quoting :off(default) | :ansi | :mysql | :mssql
       :adapter adapter }

   See hugsql.core/def-db-fns for :quoting and :adapter details."
  ([s] (map-of-db-fns-from-string &form &env s {}))
  ([s options]
   `(let [~'pdefs (parsed-defs-from-string ~s)]
      (doseq [~'pdef ~'pdefs]
        (validate-parsed-def! ~'pdef)
        (compile-exprs ~'pdef))
      (apply merge
             (map
              #(if (snippet-pdef? %)
                 (sqlvec-fn-map % ~options)
                 (db-fn-map % ~options))
              ~'pdefs)))))

(defn db-run
  "Given a database spec/connection, sql string,
   parameter data, and optional command, result,
   and options, run the sql statement"
  ([db sql] (db-run db sql :default :default {} {}))
  ([db sql param-data] (db-run db sql param-data :default :default {}))
  ([db sql param-data command] (db-run db sql param-data command :default {}))
  ([db sql param-data command result] (db-run db sql param-data command result {}))
  ([db sql param-data command result options & command-options]
   (let [f (db-fn sql command result options)]
     (f db param-data command-options))))
