---
sidebar_position: 8
---

# Select

```sql title="SQL"
-- A ":result" value of ":*" specifies a vector of records
-- (as hashmaps) will be returned
-- :name all-characters :? :*
-- :doc Get all characters
select * from characters
order by id

-- A ":result" value of ":1" specifies a single record
-- (as a hashmap) will be returned
-- :name character-by-id :? :1
-- :doc Get character by id
select * from characters
where id = :id

-- :name character-by-name :? :1
-- :doc Get character by case-insensitive name
select * from characters
where upper(name) = upper(:name)

-- :name characters-by-name-like :?
-- :doc Get characters by name like, :name-like should include % wildcards
select * from characters
where name like :name-like

-- Let's specify some columns with the
-- identifier list parameter type :i* and
-- use a value list parameter type :v* for SQL IN()
-- :name characters-by-ids-specify-cols :? :*
-- :doc Characters with returned columns specified
select :i*:cols from characters
where id in (:v*:ids)
```

```clojure title="Clojure"
(characters/all-characters-sqlvec)  ;;=>
["select * from characters
  order by id"]

(characters/all-characters db)  ;;=>
({:id 1,
  :name "Westley",
  :specialty "love",
  :created_at #inst "2015-11-09T19:33:58.472000000-00:00"}
 {:id 2,
  :name "Buttercup",
  :specialty "beauty",
  :created_at #inst "2015-11-09T19:33:58.492000000-00:00"}
 {:id 3,
  :name "Vizzini",
  :specialty "boasting",
  :created_at #inst "2015-11-09T19:33:58.530000000-00:00"}
 {:id 4,
  :name "Fezzik",
  :specialty "strength",
  :created_at #inst "2015-11-09T19:33:58.530000000-00:00"}
 {:id 5,
  :name "Inigo Montoya",
  :specialty "swordmanship",
  :created_at #inst "2015-11-09T19:33:58.530000000-00:00"})

(characters/character-by-id-sqlvec {:id 1})  ;;=>
["select * from characters
  where id = ?"
,1]

(characters/character-by-id db {:id 1})  ;;=>
{:id 1,
 :name "Westley",
 :specialty "love",
 :created_at #inst "2015-11-09T19:33:58.472000000-00:00"}

(characters/character-by-name-sqlvec {:name "buttercup"})  ;;=>
["select * from characters
  where upper(name) = upper(?)"
,"buttercup"]

(characters/character-by-name db {:name "buttercup"})  ;;=>
{:id 2,
 :name "Buttercup",
 :specialty "beauty",
 :created_at #inst "2015-11-09T19:33:58.492000000-00:00"}

(characters/characters-by-name-like-sqlvec {:name-like "%zz%"})  ;;=>
["select * from characters
  where name like ?"
,"%zz%"]

(characters/characters-by-name-like db {:name-like "%zz%"})  ;;=>
({:id 3,
  :name "Vizzini",
  :specialty "boasting",
  :created_at #inst "2015-11-09T19:33:58.530000000-00:00"}
 {:id 4,
  :name "Fezzik",
  :specialty "strength",
  :created_at #inst "2015-11-09T19:33:58.530000000-00:00"})

(characters/characters-by-ids-specify-cols-sqlvec
 {:ids [1 2], :cols ["name" "specialty"]})  ;;=>
["select name, specialty from characters
  where id in (?,?)"
,1
,2]

(characters/characters-by-ids-specify-cols
 db
 {:ids [1 2], :cols ["name" "specialty"]})  ;;=>
({:name "Westley", :specialty "love"}
 {:name "Buttercup", :specialty "beauty"})
```
