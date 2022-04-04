---
sidebar_position: 2
---

# Command

The `:command` specifies the underlying database command to run for the given SQL. The built-in values are:

- `:query` or `:?` Query with a result-set (default)
- `:execute` or `:!` Any statement
- `:returning-execute` or `:<!` Support for `INSERT ... RETURNING`
- `:insert` or `:i!` Support for insert and jdbc `.getGeneratedKeys`

`:query` and `:execute` mirror the distinction between `query` and `execute!` in the `clojure.java.jdbc` library and `fetch` and `execute` in the `clojure.jdbc` library.

For more information about `:returning-execute` and `:insert`, see [Insert](/using-hugsql/insert).

`:query` is the default command when no command is specified.

:::tip

To save some typing, the command can be specified as the second value for the `:name` key:

```sql title="SQL"
-- :name all-characters :?
```

:::

You can create command functions of your own by implementing a `hugsql.core/hugsql-command-fn` multimethod.
