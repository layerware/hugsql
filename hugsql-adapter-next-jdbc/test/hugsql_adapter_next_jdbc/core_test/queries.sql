-- :name create-colors-table :!
-- :doc create a test table of colors
create table if not exists colors (
  id   integer auto_increment primary key,
  name varchar(32),
  r    integer,
  g    integer,
  b    integer
);

-- :name drop-colors-table :!
drop table if exists colors;

-- :name insert-color
-- :command :execute
-- :result :raw
-- :doc insert a color into the colors table
insert into colors (name, r, g, b)
values (:name, :r, :g, :b);

-- :name insert-color-alt :i!
-- :doc insert a color into the colors table returning keys
insert into colors (name, r, g, b)
values (:name, :r, :g, :b);

-- :name update-color-affected :! :n
update colors
set r = :r;

-- :name delete-color-by-id :!
-- :doc delete a color by the given `id`
delete from colors where id = :id;

-- :name select-color-by-id :? :1
-- :doc select a color by id
select id as "color/id"
from colors
where id = :id
limit 1;

-- :name select-all-colors :? :*
-- :doc select all colors
select id as "color/id"
from colors;