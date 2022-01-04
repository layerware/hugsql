---
sidebar_position: 7
---

# Delete

```sql title="SQL"
-- :name delete-character-by-id :! :n
delete from characters where id = :id
```

```clojure title="Clojure"
(let [vizzini (characters/character-by-name db {:name "vizzini"})]
  (characters/delete-character-by-id-sqlvec {:id (:id vizzini)}))  ;;=>
["delete from characters where id = ?",3]

(let [vizzini (characters/character-by-name db {:name "vizzini"})]
  (characters/delete-character-by-id db {:id (:id vizzini)}))  ;;=>
1
```
