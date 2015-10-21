# Hug SQL

![alt tag](https://raw.github.com/layerware/hugsql/master/logo/hugsql_alpha_128.png)

A Clojure library for embracing SQL.

**hugsql** takes the opinionated position that SQL is the right tool for
the job when working with a relational database.  Embrace SQL!

**hugsql** uses simple conventions in your SQL files to define (at
  compile time) database functions in your Clojure namespace, creating
  a clean separation of Clojure and SQL code.

**hugsql** supports run-time replacement of SQL value parameters
(e.g., `where id = :id`), SQL identifiers (i.e. table/column names),
and raw SQL keywords.  You can also implement your own parameter
types.

**hugsql** has protocol-based adapters supporting multiple database
libraries and ships with adapters for
[clojure.java.jdbc](https://github.com/clojure/java.jdbc) (default)
and [clojure.jdbc](http://funcool.github.io/clojure.jdbc/latest/)

## Installation

**hugsql** *is a work in progress and not yet released.  Do not use yet!*

[Leiningen](https://github.com/technomancy/leiningen) dependency information:

#### Simple path:

```clj
[com.layerware/hugsql "0.1.0-SNAPSHOT"]
```

You will also need to specify your JDBC driver dependency from one of the following:

* [Apache Derby](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.apache.derby%22%20AND%20a%3A%22derby%22)
* [HSQLDB](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22hsqldb%22%20AND%20a%3A%22hsqldb%22)
* [MS SQL Server jTDS](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22net.sourceforge.jtds%22%20AND%20a%3A%22jtds%22)
* [MySQL](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22mysql%22%20AND%20a%3A%22mysql-connector-java%22)
* [PostgreSQL](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.postgresql%22%20AND%20a%3A%22postgresql%22)
* [SQLite](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.xerial%22%20AND%20a%3A%22sqlite-jdbc%22)

For example, the Postgresql driver:

```clj
[org.postgresql/postgresql "9.4-1201-jdbc41"]
```

#### More advanced options:

The `hugsql` clojar is the default meta clojar that pulls in
`hugsql-core` and the default adapter `hugsql-adapter-clojure-java-jdbc`,
which uses [clojure.java.jdbc](https://github.com/clojure/java.jdbc)
to run database queries.

If you wish to use a different adapter, you should bypass the `hugsql`
clojar and specify `hugsql-core` and the adapter clojar you desire:

```clj
[com.layerware/hugsql-core "0.1.0-SNAPSHOT"]
[com.layerware/hugsql-adapter-clojure-jdbc "0.1.0-SNAPSHOT"]
```

You can also use **hugsql** without an adapter if you only intend on
using `hugsql.core/def-sqlvec-fns` and not `hugsql.core/def-db-fns`.
For this case, you only need the `hugsql-core` clojar:

```clj
[com.layerware/hugsql-core "0.1.0-SNAPSHOT"]
```


## Getting Started

### Start with SQL

Create a file under <projdir>/resources/..., or <projdir>/src/..., or
elsewhere in your classpath:

```sql
-- src/app/db/sql/employees.sql

-- This is a regular SQL comment and ignored by hugsql,
-- however, the line starting with "-- :" is special.

-- :name employees :? :*
-- :doc Get all employees
select * from employees

-- :name get-employee :? :1
-- :doc Get employee by id
select * from employees where id = :id;
```

You probably noticed the strange `:?`, `:*` and `:1` after the query
names.  Don't worry, this is covered below in the Detailed
Overview. Press on!

### Now write some Clojure

Require **hugsql** in your namespace and call `def-db-fns` with the sql
file path:

```clj
(ns app.db.employees
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "app/db/sql/employees.sql")
```

Use the functions created in your namespace.  REPL example here:
```
(require '[app.db.employees :as emp])

; db must be a db-spec or connection
; (def db {...})

(emp/employees db)
; => [{:id 1 :name "Vizzini" :title "Genius"},
      {:id 2 :name "Fezzik" :title "Giant"},
      {:id 3 :name "Inigo Montoya" :title "Swordsman"}]

; hugsql matches parameters to keys in a hash-map
(emp/get-employee db {:id 2})
; => {:id 2 :name "Fezzik" :title "Giant"}
```

## Detailed Overview

You've only scratched the surface of **hugsql**'s functionality.  Keep
reading!

### SQL

**hugsql** encourages SQL, DDL, and DML statements to be stored in SQL
files such that you are not concatenating large strings or needing to
use leaky-abstraction DSLs in your Clojure projects.

In order to generate the Clojure functions from your SQL statements,
**hugsql** requires a simple set of conventions in your SQL files.  These
conventions allow **hugsql** to:

 - define functions by name
 - add a docstring to the defined function
 - determine how to execute the following
   - SQL select
   - DDL create table/index/view, drop ...
   - DML insert, update, delete
   - any other statements (e.g. `vacuum analyze`)
 - determine the result's return type
   - one row (hash-map)
   - many rows (vector of hash-maps)
   - boolean (for success/failure)
   - any other result-type you implement
 - replace parameters for:
   - values: `where id = :id`
   - value lists: `where id in (:v*:ids)`
   - identifiers: `from :i:table-name`
   - identifier lists: `select :i*:column-names`
   - raw SQL: `:sql:my-query`


#### SQL: hugsql special comments

**hugsql** SQL files contain special single and multi-line comments in the
following forms:

```
--:key value1 value2 value3
OR
/*:key
value1
value2
value3
*/
```

Examples:

```sql
-- regular SQL comment ignored by hugsql
/*
regular SQL multi-line comment ignored by hugsql
*/

--:name query-get-many :? :*
--:doc My query doc string to end of this line
select * from my_table;

--:name query-get-one :? :1
/*:doc
My multi-line
comment doc string
*/
select * from my_table limit 1

```

**hugsql** recognizes the following keys:
 - `:name` name of the function to create and,
           optionally, query-type and result-type
           as a short-cut for providing these as
           separate key/value pairs
 - `:doc` docstring for the created function
 - `:query-type` type of query from one of:
     - `:query`, `:?` query with a result-set
     - `:execute`, `:!` any statement
 - `:result-type` return type from one of:
     - `:one`, `:1` one row as a hash-map
     - `:many`, `:*` many rows as a vector of hash-maps
     - `:affected`, `:n` rows affected by query
     - `:boolean` true or false success indicator

#### SQL: Query Type

TODO

#### SQL: Result Type

TODO


#### SQL: hugsql Parameter Types

Within an SQL statement itself, **hugsql** understands several types of
parameters that can be passed in during the function call. All
parameter types take the form:

```
:param-name
OR
:param-type:param-name
```

When a **hugsql**-generated function is called, the parameters in an SQL
statement are replaced at runtime with the hash-map data passed in as
the function's second argument. The keys of the hash-map are matched
to the :param-name portion of the parameter. Parameters can be
repeated throughout an SQL statement, and all instances of a parameter
will be replaced at runtime.

**hugsql** recognizes the need for different types of parameters in SQL
statements.  Specifically, SQL treats data values differently from
identifiers and SQL keywords.  If you are building dynamic queries to
select specific columns or choose the `order by` direction, then a
simple value parameter is not sufficient. **hugsql** supports value
parameters, identifier parameters, and raw sql (keyword) parameters.

The built-in parameter types are detailed below along with information
on extending these types and creating new types.

##### Value Parameters

Value Parameters are replaced at runtime with the appropriately
encoded or quoted SQL value for the given Clojure data type. For
instance, a Clojure String will be single-quoted in the SQL output.

Value Parameters' type is `:value`, or `:v` for short.

Value Parameters are the default parameter type, so you can omit the
type portion of the parameter placeholder in your SQL statements.

An example:

```sql
--:name value-param :? :*
select * from example where id = :id

--name value-param-with-param-type :? :*
select * from example where id = :v:id
```

```clj
(value-param db {:id 42})
(value-param-with-param-type db {:id 42})
```

resulting SQL:

```sql
select * from example where id = 42
```

##### Value List Parameters

Value List Parameters are similar to value parameters, but work on
lists of values needed for `in (...)` queries.

Value List Parameters' type is `:value*`, or `:v*` for short.

The `*` indicates a sequence of zero or more values.

Each value in the list is treated as a value parameter, and the
list is joined with a comma.

```sql
--:name value-list-param :? :*
select * from example where name in (:v*:names)
```

```clj
(value-list-param db {:names ["Fezzik" "Vizzini"]})
```

resulting SQL:

```sql
select * from examples where name in ('Fezzik', 'Vizzini')
```

##### Identifier Parameters

Identifier Parameters are replaced at runtime with the appropriately
encoded or quoted SQL identifier for the given Clojure data type. For
instance, a table name as a Clojure String may be double-quoted in the
resulting SQL (depending on your database identifier quoting rules).

Identifier Parameters' type is `:identifier`, or `:i` for short.

```sql
--:name identifier-param :? :*
select * from :i:table-name
```

```clj
(identifier-param db {:table-name "example"})
```

resulting SQL:

```sql
select * from "example"
```

##### Identifier List Parameters

Identifier List Parameters are similar to identifier parameters, but
work on lists of identifiers.  You might use these to replace column
lists found in `select`, `group by`, `order by` clauses.

Identifier List Parameter's type is `:identifier*`, or `:i*` for
short.

The `*` indicates a sequence of zero or more identifiers.

Each identifier in the list is treated as an identifier
parameter, and the list is joined with a comma.

```sql
--:name identifier-list-param :? :*
select :i*:column-names, count(*) as population
from example
group by :i*:column-names
order by :i*:column-names
```

```clj
(identifier-list-param db {:column-names ["state" "city"]})
```

resulting SQL:

```sql
select "state", "city", count(*) as population
from example
group by "state", "city"
order by "state", "city"
```

##### Raw SQL Parameters

Raw SQL Parameters allow full, un-quoted, parameter replacement with
raw SQL, allowing you to parameterize SQL keywords (and any other SQL
parts). You might use this to set `asc` or `desc` on an `order by`
column clause, or you can use this to compose many SQL statements into
a single statement.

*With great power, comes great responsibility -- Uncle Ben*

You should take special care to always properly validate any incoming
user values before using Raw SQL Parameters to prevent an SQL
injection security issue.

Raw SQL Parameters' type is `:sql`

```sql
--:name sql-keyword-param :? :*
select * from example
order by last_name :sql:last_name_sort
```

```clj
(def user-input "asc")
(defn validated-asc-or-desc [x] (if (= x "desc") "desc" "asc"))
(sql-keyword-param db {:last_name_sort (validated-asc-or-desc user-input)})
```

resulting SQL

```sql
select * from example
order by last_name asc
```


## Documentation

[API Docs] (http://layerware.github.io/hugsql)


## License

Copyright © 2015 Layerware, Inc.

Distributed under the [Apache License, Version 2.0] (http://www.apache.org/licenses/LICENSE-2.0.html)
