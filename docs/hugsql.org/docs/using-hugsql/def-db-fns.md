---
sidebar_position: 1
---

# def-db-fns

`hugsql.core/def-db-fns` creates functions in your Clojure namespace based on the SQL queries and statements in your HugSQL-flavored SQL file.

`hugsql.core/def-db-fns` doc:

```clojure
=> (doc hugsql.core/def-db-fns)
-------------------------
hugsql.core/def-db-fns
([file] [file options])
Macro
  Given a HugSQL SQL file, define the database
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
     :ansi double-quotes: "identifier"
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
   calls (overriding set-adapter! and the :adapter option here).
```

The functions defined by def-db-fns have the following arities:

```clojure
[db]
[db params]
[db params options & command-options]
```

where:

- `db` is a db-spec, a connection, a connection pool, or a transaction object
- `param-data` is a hashmap of parameter data where the keys match parameter placeholder names in your SQL
- `options` is a hashmap of HugSQL-specific options (e.g., `:quoting` and `:adapter`)
- `& command-options` is a variable number of options to be passed down into the underlying adapter and database library functions. See [Advanced Usage](using-hugsql/advanced-usage.md) for more detail.
