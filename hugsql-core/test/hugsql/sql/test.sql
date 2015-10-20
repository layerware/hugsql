-- :name no-params-select
-- :doc No params
select * from test

-- :name one-value-param
-- :doc One value param
select * from test where id = :id

-- :name multi-value-params
-- :doc Multi value params
select * from test
where id = :id
and name = :name

-- :name value-list-param
-- :doc Value List Param
select * from test
where id in (:v*:ids)

-- :name identifier-param
-- :doc Identifier param
select * from :i:table-name

-- :name identifier-param-list
-- :doc Identifier param list
select :i*:columns from test

-- :name sql-param
-- :doc Raw SQL param
select * from test order by id :sql:id-order


-- :name create-test-table
-- :command :execute
-- :result :affected
-- :doc Create test table
create table test (
  id     integer,
  "name" varchar(20)
)

-- :name insert-into-test-table :! :n
insert into test (id, "name") values (:id, :name)

-- :name update-test-table :! :n
update test set "name" = :name where id = :id

-- :name select-one-test-by-id :? :1
select * from test where id = :id

-- :name drop-test-table :! :n
-- :doc Drop test table
drop table test
