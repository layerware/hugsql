-- testing




-- :name no-params-select
-- :doc No params
select * from test

-- testing

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

-- :name tuple-param
-- :doc Tuple Param
select * from test
where (id, name) = :tuple:id-name

-- :name tuple-param-list
-- :doc Tuple Param List
insert into test (id, name)
values :t*:people

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
  id    integer primary key,
  name  varchar(20)
)

-- :name create-test-table-mysql :! :n
-- :doc Create test table
create table test (
  id    integer auto_increment primary key,
  name  varchar(20)
)

-- :name create-test-table-h2 :! :n
-- :doc Create test table
create table test (
  id    integer auto_increment primary key,
  name  varchar(20)
)

-- :name create-test-table-hsqldb :! :n
-- :doc Create test table
create table test (
  id    integer identity primary key,
  name  varchar(20)
)

-- :name insert-into-test-table :! :n
-- :doc insert with a regular execute
insert into test (id, name) values (:id, :name)

-- :name insert-into-test-table-returning :<!
-- :doc insert with an sql returning clause
-- only some db's support this
insert into test (id, name) values (:id, :name) returning id

-- :name insert-into-test-table-return-keys :insert :raw
-- behavior of this adapter-specific and db-specific
insert into test (id, name) values (:id, :name)

-- :name insert-multi-into-test-table :! :n
insert into test (id, name) values :tuple*:values

-- :name update-test-table :! :n
update test set name = :name where id = :id

-- :name update-test-table-returning :<! :1
update test set name = :name where id = :id returning id

-- :name- update-test-table-returning-private :<! :1
update test set name = :name where id = :id returning id

-- :name select-one-test-by-id :? :1
select * from test where id = :id

-- :name select-ordered :?
select :i*:cols from test
order by :i*:sort-by

-- :name select-deep-get :? :1
select * from test
where id = :records.0.id

-- :name select-namespaced-keyword :? :1
select * from test
where id = :test/id

-- :name select-namespaced-keyword-deep-get :? :1
select * from test
where id = :test.x/records.0.id

-- :name drop-test-table :! :n
-- :doc Drop test table
drop table test

-- :name- a-private-fn :? :*
-- notice the dash suffix on :name- above
select * from test

-- :name another-private-fn :? :*
-- :meta {:private true}
select * from test

-- :name user-meta
/* :meta {:one 1
          :two 2
          :three 3} */
select * from test

-- :name clj-expr-single :? :1
-- clj expression is expected to return
-- a string or nil
-- A single-line comment expects a full
-- expression within the line
-- The string returned from the expression
-- is parsed with a second pass of the hugsql
-- parser at run-time, picking up any hugsql parameters
select
--~ (if (seq (:cols params)) ":i*:cols" "*")
from test
order by id

-- :name clj-expr-multi :? :1
--  A multi-line comment clj expression can have
-- interspersed sql.  The clj expression
-- starts with /*~, all "continuing" parts
-- start with /*~, and the expression ends with ~*/
-- When a clj expression needs to represent
-- advancing to the  "next" form as in the
-- if expression below,then an empty
-- separator /*~*/ is needed:
select
/*~ (if (seq (:cols params)) */
:i*:cols
/*~*/
*
/*~ ) ~*/
from test
order by id

-- :name clj-expr-multi-when :? :1
select * from test
/*~ (when (:id params) */
where id = :id
/*~ ) ~*/
order by id

-- :name clj-expr-generic-update :! :n
/* :require [clojure.string :as string]
            [hugsql.parameters :refer [identifier-param-quote]] */
update :i:table set
/*~
(string/join ","
  (for [[field _] (:updates params)]
    (str (identifier-param-quote (name field) options)
      " = :v:updates." (name field))))
~*/
where id = :id

/* Snippets */

-- :snip select-snip
select :i*:cols

-- :snip from-snip
from :i*:tables

-- :snip where-snip
where :snip*:cond

-- :snip order-snip
order by :i*:fields

-- :snip cond-snip
-- We could come up with something
-- quite elaborate here with some custom
-- parameter types that convert := to =, etc.,
-- but for the purposes of testing snippets,
-- we are passing through sql params
-- Examples:
-- {:conj "and" :cond ["id" "=" 1]}
-- OR
-- {:conj "or" :cond ["id" "=" 1]}
-- note that :conj can be "", too
:sql:conj :i:cond.0 :sql:cond.1 :v:cond.2

-- :name snip-query :? :*
:snip:select
:snip:from
--~ (when (:where params) ":snip:where")
:snip:order

/* Fragments */

-- :frag select-frag
select id, name

-- :frag from-frag
from test

-- :frag where-frag-0
where true

-- :frag where-frag-1
and id = :id

-- :frag where-frag-2
and name = :name

-- :frag where-frag
:frag:where-frag-0
:frag:where-frag-1
:frag:where-frag-2


-- :name frag-query :? :*
:frag:select-frag
:frag:from-frag
:frag:where-frag

-- :name frag-query-cond :? :*
:frag:select-frag
:frag:from-frag
:frag:where-frag-0
--~ (when (:id params) ":frag:where-frag-1")
--~ (when (:name params) ":frag:where-frag-2")

-- :frag where-frag-cond
--~ (when (:id params) ":frag:where-frag-1")
--~ (when (:name params) ":frag:where-frag-2")

-- :name frag-query-cond-2 :? :*
:frag:select-frag
:frag:from-frag
:frag:where-frag-0
--~ (when (or (:id params) (:name params)) ":frag:where-frag-cond")
