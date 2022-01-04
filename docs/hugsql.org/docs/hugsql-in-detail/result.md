---
sidebar_position: 3
---

# Result

The `:result` specifies the expected result type for the given SQL. The available built-in values are:

- `:one` or `:1` One row as a hash-map.
- `:many` or `:*` Many rows as a vector of hash-maps.
- `:affected` or `:n` Number of rows affected (inserted/updated/deleted).
- `:raw` Passthrough an untouched result.

`:raw` is the default when no result is specified.

:::tip

To save some typing, the result function can be specified as the third value for the `:name` key. You must supply a command value in order to use this shorthand convention:

```sql title="SQL"
-- :name all-characters :? :*
```

:::

You can create result functions of your own by implementing a `hugsql.core/hugsql-result-fn` multimethod.
