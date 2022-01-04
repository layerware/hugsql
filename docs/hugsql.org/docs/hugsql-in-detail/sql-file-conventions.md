---
sidebar_position: 1
---

# SQL File Conventions

HugSQL SQL files contain special single-line comments and multi-line comments in the following forms:

```sql title="SQL"
 -- :key value1 value2 value3
 OR
 /* :key
 value1
 value2
 value3
 */
```

Examples:

```sql title="SQL"
-- regular SQL comment ignored by hugsql
/*
regular SQL multi-line comment ignored by hugsql
*/

-- :name query-get-many :? :*
-- :doc My query doc string to end of this line
select * from my_table;

-- :name query-get-one :? :1
/* :doc
My multi-line
comment doc string
*/
select * from my_table limit 1
```

HugSQL recognizes the following keys:

- `:name` or `:name-` (private fn) Name of the function to create. Optionally followed by the command and result (e.g., `:name :? :*`) instead of providing these as separate key/value pairs.
- `:doc` Docstring for the created function.
- `:command` Underlying database command to run.
- `:result` Expected result type (shape).
- `:snip` or `:snip-` (private fn) Name of the function to create. `:snip` is used in place of `:name` for snippets.
- `:meta` Metadata in the form of an EDN hashmap to attach to function.
- `:require` Namespace require and aliases for Clojure expression support.
