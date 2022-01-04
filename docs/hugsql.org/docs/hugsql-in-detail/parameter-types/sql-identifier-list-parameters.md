---
sidebar_position: 6
---

# SQL Identifier List Parameters

Identifier List Parameters are similar to Identifier Parameters, but work on lists of identifiers. You might use these to replace column lists found in `select`, `group by`, or `order by` clauses.

Identifier List Parameter's type is `:identifier*`, or `:i*` for short.

The `*` indicates a sequence of zero or more identifiers.

Each identifier in the list is treated as an Identifier Parameter, and the list is joined with a comma.

```sql title="SQL"
--:name identifier-list-param :? :*
select :i*:column-names, count(*) as population
from example
group by :i*:column-names
order by :i*:column-names
```

```clojure title="Clojure"
(identifier-list-param-sqlvec {:column-names ["state" "city"]})
;=> ["select state, city, count(*) as population\n
;     from example\n
;     group by state, city\n
;     order by state, city"]
```

As of HugSQL 0.4.6, Identifier List Parameters support SQL aliases:

```sql title="SQL"
--:name identifier-list-param :? :*
select :i*:column-names-aliased, count(*) as population
from example
group by :i*:column-names
order by :i*:column-names
```

```clojure title="Clojure"
(let [columns [["state" "my_state"] ["city" "my_city"]]]
  (identifier-list-param-sqlvec {:column-names-aliased columns
                                 :column-names (mapv first columns)}))
;=> ["select state as my_state, city as my_city, count(*) as population\n
;     from example\n
;     group by state, city\n
;     order by state, city"]
```
