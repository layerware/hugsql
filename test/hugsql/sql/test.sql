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

-- :name identifier-param
-- :doc Identifier param
select * from :i:table-name
