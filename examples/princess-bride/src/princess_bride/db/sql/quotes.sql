-- src/princess_bride/db/sql/quotes.sql
-- The Princess Bride Quotes

-- :name create-quotes-table :!
-- :doc Create quotes table
-- auto_increment and current_timestamp below are h2-database functions
create table quotes (
  id           integer auto_increment primary key,
  character_id integer,
  text         varchar(40),
  created_at   timestamp not null default current_timestamp
)

-- :name drop-quotes-table :!
-- :doc Drop quotes table if exists
drop table if exists quotes

-- :name all-quotes :? :*
-- :doc All quotes
select * from quotes order by id

-- :name quotes-by-character :? :*
-- :doc Quotes by character
select * from quotes
where character_id = :id ?
