---
sidebar_position: 5
---

# SQL Identifier Parameters

Identifier Parameters are replaced at runtime with an optionally-quoted SQL identifier.

Identifier Parameters' type is `:identifier`, or `:i` for short.

```sql title="SQL"
--:name identifier-param :? :*
select * from :i:table-name
```

```clojure title="Clojure"
(identifier-param-sqlvec {:table-name "example"})
;=> ["select * from example"]
```

As of HugSQL 0.4.6, Identifier Parameters support SQL aliases:

```clojure title="Clojure"
(identifier-param-sqlvec {:table-name ["example" "my_example"]})
;=> ["select * from example as my_example"]
```

By default, identifiers are not quoted. You can specify your desired quoting as an option when defining your functions or as an option when calling your function.

:::danger
If you are taking identifiers from user input, you should use the `:quoting` option to properly quote and escape identifiers to prevent SQL injection!
:::

Valid `:quoting` options provided to `hugsql.core/def-db-fns` (and friends) are:

- `:ansi` double-quotes: `"identifier"`
- `:mysql` backticks: `` `identifier` ``
- `:mssql` square brackets: `[identifier]`
- `:off` no quoting (default)

Identifiers containing a period/dot . are split, quoted separately, and then rejoined. This supports myschema.mytable conventions.

```clojure title="Clojure"
(hugsql.core/def-db-fns "path/to/good.sql" {:quoting :ansi})

(identifier-param-sqlvec {:table-name "example"})
;=> ["select * from \"example\""]

(identifier-param-sqlvec {:table-name "schema1.example"} {:quoting :mssql})
;=> ["select * from [schema1].[example]"]
```
