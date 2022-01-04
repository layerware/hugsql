---
sidebar_position: 4
---

# DDL (Create, Drop)

```sql title="SQL"
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
```

```clojure title="Clojure"
(characters/create-characters-table-sqlvec)  ;;=>
["create table characters (
    id         integer auto_increment primary key,
    name       varchar(40),
    specialty  varchar(40),
    created_at timestamp not null default current_timestamp
  )"]

(characters/create-characters-table db)  ;;=>
[0]

(characters/drop-characters-table-sqlvec)  ;;=>
["drop table if exists characters"]

(characters/drop-characters-table db)  ;;=>
[0]
```