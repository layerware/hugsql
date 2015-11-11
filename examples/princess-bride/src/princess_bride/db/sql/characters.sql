-- src/princess_bride/db/sql/characters.sql
-- The Princess Bride Characters

-- :name create-characters-table
-- :command :execute
-- :result :raw
-- :doc Create characters table
--  auto_increment and current_timestamp are
--  H2 Database specific (adjust to your DB)
create table characters (
  id         integer auto_increment primary key, 
  name       varchar(40),
  specialty  varchar(40),
  created_at timestamp not null default current_timestamp
)

/* The create-character-table definition above uses the full,
long-hand "-- :key :value" syntax to specify the :command and
:result.  We can save some typing by using the short-hand notation
as the second and (optionally) third values for the :name.  Below, the
:! is equivalent to ":command :!", where :! is an alias for
:execute.  The default :result is :raw when not specified, so
there is no need to specify it as the third value. */

-- :name drop-characters-table :!
-- :doc Drop characters table if exists
drop table if exists characters

-- A :result value of :n below will return affected row count:
-- :name insert-character :! :n
-- :doc Insert a single character
insert into characters (name, specialty)
values (:name, :specialty)

-- :name insert-characters :! :n
-- :doc Insert multiple characters with :tuple* parameter type
insert into characters (name, specialty)
values :tuple*:characters

-- :name update-character-specialty :! :n
update characters
set specialty = :specialty
where id = :id

-- :name delete-character-by-id :! :n
delete from characters where id = :id

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

