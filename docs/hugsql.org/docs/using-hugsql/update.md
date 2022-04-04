---
sidebar_position: 6
---

# Update

```sql title="SQL"
-- :name update-character-specialty :! :n
update characters
set specialty = :specialty
where id = :id
```

```clojure title="Clojure"
(let [vizzini (characters/character-by-name db {:name "vizzini"})]
 (characters/update-character-specialty-sqlvec
  {:id (:id vizzini), :specialty "boasting"}))  ;;=>
["update characters
  set specialty = ?
  where id = ?"
,"boasting"
,3]

(let [vizzini (characters/character-by-name db {:name "vizzini"})]
 (characters/update-character-specialty db
  {:id (:id vizzini), :specialty "boasting"}))  ;;=>
1
```
