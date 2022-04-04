---
sidebar_position: 4
---

# SQL Tuple List Parameters

Tuple List Parameters support lists of tuples. This is specifically useful for multi-record insert.

Tuple List Parameters' type is `:tuple*`, or `:t*` for short.

Each tuple in the list is treated as a Tuple Parameter. The list is joined with a comma.

```sql title="SQL"
-- :name tuple-param-list
-- :doc Tuple Param List
insert into test (id, name)
values :t*:people
```

```clojure title="Clojure"
(tuple-param-list-sqlvec {:people [[1 "Ed"] [2 "Al"] [3 "Bo"]]})
;=> ["insert into test (id, name)\nvalues (?,?),(?,?),(?,?)" 1 "Ed" 2 "Al" 3 "Bo"]
```

:::note
The use of a tuple list in the above manner is not supported by all databases. Postgresql, MySQL, H2, Derby, and SQLite support it. HSQLDB does not support it.
:::

:::caution
BATCH INSERTS: It should be noted that Tuple List Parameter support is only support for SQL `INSERT...VALUES (...),(...),(...)` syntax. This is appropriate for small-ish multi-record inserts. However, this is different than large batch support. The underlying JDBC driver for your database has a limit to the size of the SQL and the number of allowed bind parameters. If you are doing large batch inserts, you should `map` or `doseq` over your HugSQL-generated insert function within a [transaction](/using-hugsql/transactions).
:::