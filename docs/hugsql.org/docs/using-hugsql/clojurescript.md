---
sidebar_position: 12
---

# ClojureScript

`hugsql-core` and `hugsql-adapter` are `.cljc`: the parser, parameters,
sqlvec fns and db fns run on ClojureScript too. There is no `eval` and no
file system at run time there, so the functions that define vars from a
file or a string are macros in `hugsql.macros`:

```clojure
(ns my.db
  (:require [hugsql.core :as hugsql]
            [hugsql.macros :refer [def-db-fns def-sqlvec-fns]]))

(def-db-fns "my/db.sql" {:adapter my-adapter})
(def-sqlvec-fns "my/db.sql")
```

`hugsql.macros` has `def-db-fns`, `def-sqlvec-fns`,
`def-db-fns-from-string`, `def-sqlvec-fns-from-string`, `map-of-db-fns`
and `map-of-sqlvec-fns`. On Clojure each expands to the `hugsql.core` fn
of the same name, so a `.cljc` namespace can use them on both platforms.

On ClojureScript:

- The SQL is read and parsed when the namespace compiles. A file is a
  literal string naming a path on the classpath; a `-from-string` SQL
  string is a literal string.
- `options` is evaluated at run time, except `:fn-suffix`, which names
  the fns and is read at compile time when `options` is a map literal.
- Clojure expressions (`--~`, `/*~ ~*/`) are compiled into the calling
  namespace. A `:require` header's aliases and refers are qualified in the
  expression code; the namespaces it names must already be loaded, e.g.
  required by the calling namespace. Runtime fns (`db-fn`, `sqlvec-fn`,
  `sqlvec`, `db-run`, `map-of-*-from-string`) take SQL without
  expressions.
- A `hugsql-command-fn` or `hugsql-result-fn` method returns a fn, not a
  symbol: there is no `resolve` at run time. The built-in methods' symbols
  still work.

## Adapters and promises

Database drivers on Node and in the browser are asynchronous. The adapter
protocol is unchanged, and an adapter's `execute` and `query` may return a
promise; its `result-one`, `result-many`, `result-affected` and
`result-raw` then receive that promise and return one. `on-exception`
sees only exceptions thrown synchronously, so an adapter handles its own
rejections. HugSQL ships no ClojureScript adapter.

## Reloading with shadow-cljs

Under [shadow-cljs](https://github.com/thheller/shadow-cljs), the macros
read the file with `shadow.resource/slurp-resource`, which records the
read: `shadow-cljs watch` recompiles and hot-reloads the namespace when its
`.sql` file changes. With shadow-cljs the path may also be relative to the
namespace (`"./db.sql"`). Other compilers read the file from the classpath
with `clojure.java.io/resource` and do not track it: recompile the
namespace yourself after editing the SQL.

## Tests

The `.cljc` tests run on both platforms. On Clojure, `lein test` runs
them with the rest; on Node:

```sh
cd hugsql-core
clojure -M:test-cljs
```
