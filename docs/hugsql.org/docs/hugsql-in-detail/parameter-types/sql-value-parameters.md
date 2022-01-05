---
sidebar_position: 1
---

# SQL Value Parameters

Value Parameters are replaced at runtime with an appropriate SQL data type for the given Clojure data type.

HugSQL defers the Clojure-to-SQL conversion to the underlying database driver using the sqlvec format.

Value Parameters' type is `:value`, or `:v` for short.

Value Parameters are the default parameter type, so you can omit the type portion of the parameter placeholder in your SQL statements.

```sql title="SQL"
--:name value-param :? :*
select * from characters where id = :id

--:name value-param-with-param-type :? :*
select * from characters where id = :v:id
```

```clojure title="Clojure"
(value-param-sqlvec {:id 42})
;=> ["select * from characters where id = ?" 42]
```
