---
sidebar_position: 7
---

# Raw SQL Parameters

Raw SQL Parameters allow full, un-quoted, parameter replacement with raw SQL, allowing you to parameterize SQL keywords (and any other SQL parts). You might use this to set `asc` or `desc` on an `order by` column clause, or you can use this to compose many SQL statements into a single statement.

:::danger
You should take special care to always properly validate any incoming user input before using Raw SQL Parameters to prevent an SQL injection security issue.
:::

SQL Parameters' type is `:sql`.

```sql title="SQL"
--:name sql-keyword-param :? :*
select * from example
order by last_name :sql:last_name_sort
```

```clojure title="Clojure"
(def user-input "asc")
(defn validated-asc-or-desc [x] (if (= x "desc") "desc" "asc"))
(sql-keyword-param-sqlvec {:last_name_sort (validated-asc-or-desc user-input)})
;=> ["select * from example\norder by last_name asc"]
```
