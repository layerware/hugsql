---
sidebar_position: 0
slug: /
---

# Introduction

![HugSQL](/img/hugsql_alpha_128.png)

**HugSQL is a Clojure library for _embracing_ SQL.**

- SQL is the right tool for the job when working with a relational database!
- HugSQL uses [simple conventions](/hugsql-in-detail) in your SQL files to define database functions in your Clojure namespace, creating a clean separation of Clojure and SQL code.
- HugSQL supports runtime replacement of:
  - [SQL Values](/hugsql-in-detail/parameter-types/sql-value-parameters) `where id = :id`
  - [SQL Value Lists](/hugsql-in-detail/parameter-types/sql-value-list-parameters) `where id in (:v*:ids)`
  - [SQL Identifiers](/hugsql-in-detail/parameter-types/sql-identifier-parameters) `from :i:table-name`
  - [SQL Identifier Lists](/hugsql-in-detail/parameter-types/sql-identifier-list-parameters) `select :i*:column-names`
  - [SQL Tuples](/hugsql-in-detail/parameter-types/sql-tuple-parameters) `where (a.id, b.id) = (:t:ids)`
  - [SQL Tuple Lists](/hugsql-in-detail/parameter-types/sql-tuple-list-parameters) `insert into emp (id, name) values (:t*:people)`
  - [Raw SQL](/hugsql-in-detail/parameter-types/sql-raw-parameters)
  - [Custom Parameter Types](/hugsql-in-detail/parameter-types/custom-parameter-types) (implement your own)
- HugSQL features [Clojure Expressions](/using-hugsql/composability/clojure-expressions) and [Snippets](/using-hugsql/composability/snippets) providing the full expressiveness of Clojure and the composability of partial SQL statements when constructing complex SQL queries.
- HugSQL has [protocol-based adapters](/hugsql-adapters) supporting multiple database libraries and ships with adapters for `clojure.java.jdbc` (default), `next.jdbc`, and `clojure.jdbc`.