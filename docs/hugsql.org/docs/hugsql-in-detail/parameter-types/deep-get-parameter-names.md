---
sidebar_position: 11
---

# Deep Get Parameter Names

The parameter name can optionally use a "deep get" syntax to drill down into a parameter data structure. This syntax consists of keywords and integers joined by a period .. A keyword is a key for a hashmap. An integer is a vector index. For example:

```sql title="SQL"
-- first-employee :? :1
select * from employees where id = :value:employees.0.id
```

```clojure title="Clojure"
(first-employee db {:employees [{:id 1} {:id 2}]})
;=> {:id 1 :name "Al"}
```
