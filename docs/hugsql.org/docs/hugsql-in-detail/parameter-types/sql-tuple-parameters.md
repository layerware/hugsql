---
sidebar_position: 3
---

# SQL Tuple Parameters

Tuple Parameters are similar to Value List Parameters in that they both work with lists of values.

Tuple Parameters differ in that they enclose their values in parentheses. Additionally, while a Tuple Parameter can be used like a Value List Parameter (e.g., for an `in (...)` condition), it is generally understood that a Tuple Parameter's data values may have different data types, but Value Parameters values are of the same data type.

Tuple Parameters' type is `:tuple`, or `:t` for short.

Each value in the list is treated as a Value Parameter. The list is joined with a comma and enclosed in parentheses.

```sql title="SQL"
-- :name tuple-param
-- :doc Tuple Param
select * from test
where (id, name) = :tuple:id-name
```

```clojure title="Clojure"
(tuple-param-sqlvec {:id-name [1 "A"]})
;=> ["select * from test\nwhere (id, name) = (?,?)" 1 "A"]
```

:::note
The use of a tuple in the above manner is not supported by all databases. Postgresql, MySQL, and H2 support it. Derby, HSQLDB, and SQLite do not support it.
:::
