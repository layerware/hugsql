---
sidebar_position: 2
---

# SQL Value List Parameters

Value List Parameters are similar to Value Parameters, but work on lists of values needed for `in (...)` queries.

Value List Parameters' type is `:value*`, or `:v*` for short.

The `*` indicates a sequence of zero or more values.

Each value in the list is treated as a value parameter, and the list is joined with a comma.

```sql title="SQL"
--:name value-list-param :? :*
select * from characters where name in (:v*:names)
```

```clojure title="Clojure"
(value-list-param-sqlvec {:names ["Fezzik" "Vizzini"]})
;=> ["select * from characters where name in (?,?)" "Fezzik" "Vizzini"]
```
