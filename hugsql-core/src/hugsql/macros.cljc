(ns hugsql.macros
  "`def-db-fns`, `def-sqlvec-fns`, their `-from-string` variants,
   `map-of-db-fns` and `map-of-sqlvec-fns` as macros, for code that runs on
   ClojureScript (or is shared, in .cljc, with Clojure).

   On Clojure each macro expands to the `hugsql.core` fn of the same name,
   so behavior is unchanged.

   On ClojureScript there is no `eval` and no file system at run time, so
   the SQL is read and parsed when the namespace is compiled:

   - `file` is a literal string, a path on the classpath; `s` (the
     `-from-string` variants) is a literal string.
   - `options` is evaluated at run time, except `:fn-suffix`, which names
     the defined fns and is read at compile time when `options` is a map
     literal.
   - Clojure expressions (`--~`, `/*~ ~*/`) are compiled into the
     namespace. Aliases and refers of a `:require` header are qualified in
     the expression code; the namespaces it names must be loaded already
     (e.g. required by the calling namespace).
   - Under shadow-cljs the file is read through
     `shadow.resource/slurp-resource`, so `shadow-cljs watch` recompiles
     and hot-reloads the calling namespace when the file changes. Other
     compilers read it from the classpath and do not track it.

   Usage:

   ```
   (ns my.db
     (:require [hugsql.core :as hugsql]
               [hugsql.macros :refer [def-db-fns]]))

   (def-db-fns \"my/db.sql\" {:adapter my-adapter})
   ```"
  (:require #?(:clj [hugsql.core :as hugsql]
               :cljs [hugsql.core])
            #?@(:clj [[clojure.java.io :as io]
                      [clojure.string :as string]
                      [clojure.walk :as walk]
                      [hugsql.parser :as parser]]))
  #?(:cljs (:require-macros [hugsql.macros])))

#?(:clj
   (do
     (defn- cljs? [env] (boolean (:ns env)))

     (defn- shadow-slurp
       "shadow-cljs's resource reader, which records the read for `watch`,
        or nil when shadow-cljs is not on the classpath."
       []
       (when (io/resource "shadow/resource.clj")
         (require 'shadow.resource)
         (resolve 'shadow.resource/slurp-resource)))

     (defn- slurp-sql [env file]
       (when-not (string? file)
         (throw (ex-info (str "On ClojureScript, the HugSQL file must be a literal string: " (pr-str file)) {})))
       (if-let [slurp-resource (shadow-slurp)]
         (slurp-resource env file)
         (if-let [r (io/resource file)]
           (slurp r)
           (throw (ex-info (str "Can not read file: " file) {})))))

     (defn- literal-string [s]
       (when-not (string? s)
         (throw (ex-info (str "On ClojureScript, the HugSQL string must be a literal string: " (pr-str s)) {})))
       s)

     (defn- require-qualifier
       "A fn qualifying a symbol by the aliases and refers of the `:require`
        header `hdr-require` (its whitespace-split strings)."
       [hdr-require]
       (let [specs (when (seq hdr-require)
                     (read-string (str "[" (string/join " " hdr-require) "]")))
             specs (map #(if (symbol? %) [%] %) specs)
             aliases (into {} (for [[lib & opts] specs
                                    :let [{:keys [as]} (apply hash-map opts)]
                                    :when as]
                                [(name as) (name lib)]))
             refers (into {} (for [[lib & opts] specs
                                   :let [{:keys [refer]} (apply hash-map opts)]
                                   :when (sequential? refer)
                                   r refer]
                               [r (symbol (name lib) (name r))]))]
         (fn [x]
           (cond
             (not (symbol? x)) x
             (namespace x) (if-let [lib (aliases (namespace x))]
                             (symbol lib (name x))
                             x)
             :else (get refers x x)))))

     (defn- expr-groups
       "The expressions of `sql-template` as `hugsql.core/expr-pass` groups
        them for `hugsql.core/run-expr`."
       [sql-template]
       (loop [[curr & pile] sql-template
              expr []
              groups []]
         (cond
           (nil? curr) groups
           (or (vector? curr) (seq expr))
           (if (and (vector? curr) (= :end (last curr)))
             (recur pile [] (conj groups (conj expr curr)))
             (recur pile (conj expr curr) groups))
           :else (recur pile expr groups))))

     (defn- expr-forms
       "Forms registering each expression of `pdef` where
        `hugsql.core/run-expr` looks it up."
       [pdef]
       (let [qualify (require-qualifier (:require (:hdr pdef)))]
         (for [group (expr-groups (:sql pdef))]
           `(swap! hugsql.expr-run/exprs assoc
                   (keyword (hugsql/expr-name '~group))
                   ~(walk/postwalk qualify (read-string (hugsql/expr-fn-str group)))))))

     (defn- fn-entry
       "[name meta fn-form] of `pdef`: a db fn when `db?` and `pdef` is no
        snippet, else an sqlvec fn."
       [pdef options compile-options db?]
       (let [db-fn? (and db? (not (hugsql/snippet-pdef? pdef)))
             fm (if db-fn?
                  (hugsql/db-fn-map pdef compile-options)
                  (hugsql/sqlvec-fn-map pdef compile-options))
             [k {:keys [meta]}] (first fm)
             nam (symbol (name k))
             sql (:sql pdef)
             options `(assoc ~options :fn-name '~nam)]
         [nam
          (dissoc meta :file :line)
          (if db-fn?
            `(hugsql/db-fn* '~sql ~(:command meta) ~(:result meta) ~options)
            `(hugsql/sqlvec-fn* '~sql ~options))]))

     (defn- parsed-defs [sql file]
       (let [pdefs (parser/parse sql {:file file})]
         (doseq [pdef pdefs] (hugsql/validate-parsed-def! pdef))
         pdefs))

     (defn- def-forms [pdefs options db?]
       (let [compile-options (if (map? options) options {})]
         `(do
            ~@(mapcat expr-forms pdefs)
            ~@(for [pdef pdefs
                    :let [[nam meta fn-form] (fn-entry pdef options compile-options db?)]]
                `(def ~(with-meta nam (assoc meta :arglists `'~(:arglists meta)))
                   ~fn-form))
            nil)))

     (defn- map-form [pdefs options db?]
       (let [compile-options (if (map? options) options {})]
         `(do
            ~@(mapcat expr-forms pdefs)
            ~(into {}
                   (for [pdef pdefs
                         :let [[nam meta fn-form] (fn-entry pdef options compile-options db?)]]
                     [(keyword nam) {:meta `'~meta :fn fn-form}])))))

     (defmacro def-db-fns
       "`hugsql.core/def-db-fns`; on ClojureScript `file` is read at compile
        time (see the namespace doc)."
       ([file] `(def-db-fns ~file {}))
       ([file options]
        (if (cljs? &env)
          (def-forms (parsed-defs (slurp-sql &env file) file) options true)
          `(hugsql/def-db-fns ~file ~options))))

     (defmacro def-sqlvec-fns
       "`hugsql.core/def-sqlvec-fns`; on ClojureScript `file` is read at
        compile time (see the namespace doc)."
       ([file] `(def-sqlvec-fns ~file {}))
       ([file options]
        (if (cljs? &env)
          (def-forms (parsed-defs (slurp-sql &env file) file) options false)
          `(hugsql/def-sqlvec-fns ~file ~options))))

     (defmacro def-db-fns-from-string
       "`hugsql.core/def-db-fns-from-string`; on ClojureScript `s` is a
        literal string, parsed at compile time."
       ([s] `(def-db-fns-from-string ~s {}))
       ([s options]
        (if (cljs? &env)
          (def-forms (parsed-defs (literal-string s) nil) options true)
          `(hugsql/def-db-fns-from-string ~s ~options))))

     (defmacro def-sqlvec-fns-from-string
       "`hugsql.core/def-sqlvec-fns-from-string`; on ClojureScript `s` is a
        literal string, parsed at compile time."
       ([s] `(def-sqlvec-fns-from-string ~s {}))
       ([s options]
        (if (cljs? &env)
          (def-forms (parsed-defs (literal-string s) nil) options false)
          `(hugsql/def-sqlvec-fns-from-string ~s ~options))))

     (defmacro map-of-db-fns
       "`hugsql.core/map-of-db-fns`; on ClojureScript `file` is read at
        compile time (see the namespace doc)."
       ([file] `(map-of-db-fns ~file {}))
       ([file options]
        (if (cljs? &env)
          (map-form (parsed-defs (slurp-sql &env file) file) options true)
          `(hugsql/map-of-db-fns ~file ~options))))

     (defmacro map-of-sqlvec-fns
       "`hugsql.core/map-of-sqlvec-fns`; on ClojureScript `file` is read at
        compile time (see the namespace doc)."
       ([file] `(map-of-sqlvec-fns ~file {}))
       ([file options]
        (if (cljs? &env)
          (map-form (parsed-defs (slurp-sql &env file) file) options false)
          `(hugsql/map-of-sqlvec-fns ~file ~options))))))
