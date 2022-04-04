---
sidebar_position: 13
---

# The Colon

Since HugSQL has commandeered the colon `:` for use, you will need to escape a colon with a backslash if you actually need a colon in your SQL. Escaping a colon will prevent HugSQL from interpreting the colon as the start of a HugSQL parameter. For example, Postgresql array ranges use colons:

```sql title="SQL"
select my_arr[1\:3] from ...
```

BUT, HugSQL does make one exception to the colon-escaping necessity in regard to the Postgresql's historical type-casting syntax that uses a double-colon `::` to indicate a Postgresql data type. So, HugSQL properly leaves alone a double-colon:

```sql title="SQL"
select id::text ...
... where id = :id::bigint
-- with the param type specified
... where id = :v:id::bigint
```
