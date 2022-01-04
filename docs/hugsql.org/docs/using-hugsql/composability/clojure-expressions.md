---
sidebar_position: 1
---

# Clojure Expressions

Clojure Expressions give HugSQL SQL statements the full power of Clojure to conditionally compose portions of SQL during runtime.

Expressions are defined and compiled just after the initial file/string parse.

Expressions are written within SQL comments (in keeping with the SQL-first workflow).

Expressions should return a `String` or `nil`. The `String` returned may contain HugSQL-specific parameter syntax.

Expressions have two bound symbols available at runtime:

- `params`, a hashmap of parameter data
- `options`, a hashmap of options

## Single-Line Comment Expression

A single-line comment expression starts with `--~`. Notice the tilde `~`. An entire Clojure expression is expected to be in a single-line comment.

```sql title="SQL"
-- :name clj-expr-single :? :1
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from test
order by id
```

## Multi-Line Comment Expression

A multi-line comment expression can have interspersed SQL. The expression:

- starts with `/*~`
- all "continuing" parts also start with `/*~`
- ends with `~*/`

When an expression needs to represent advancing to the "next" Clojure form (like the `if` below), an empty separator `/*~*/` is necessary:

```sql title="SQL"
-- :name clj-expr-multi :? :1
select
/*~ (if (seq (:cols params)) */
:i*:cols
/*~*/
*
/*~ ) ~*/
from test
order by id
```

Expressions needing to access Clojure namespaces other than the included clojure.core can specify a `:require` similar to usage in `(ns ...)`:

```sql title="SQL"
-- :name clj-expr-generic-update :! :n
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
update :i:table set
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
where id = :id
```

```clojure title="Clojure"
  (clj-expr-generic-update db {:table "test"
                               :updates {:name "X"}
                               :id 3})
```
