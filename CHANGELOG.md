# HugSQL CHANGELOG

## 0.4.2

 - fix parameter mismatch validation for false/nil (issue
   [#23](https://github.com/layerware/hugsql/issues/23))
 - fix sqlvec-fn* not propagating all options
 - add doc clarification on tuple list multi-record insert vs large
   batch insert (issue
   [#22](https://github.com/layerware/hugsql/issues/22))
 - small doc fixes

## 0.4.1

 - map-of-db-fns, map-of-sqlvec-fns & -from-string variants for easier
   custom use of hugsql-created functions (issue
   [#19](https://github.com/layerware/hugsql/issues/19)) (See
   [Other Useful Functions](http://www.hugsql.org/#using-other-fns))
 - link to postgresql async adapter fixed (pull
   [#20](https://github.com/layerware/hugsql/pull/20))
 - minor test & doc updates


## 0.4.0

 - Escape colon in SQL (issue
   [#13](https://github.com/layerware/hugsql/issues/13))
 - def-db-fns-from-string & def-sql-fns-from-string (issue
   [#16](https://github.com/layerware/hugsql/issues/16))
 - Added several functions that operate on individual SQL statements,
   which are useful at the REPL or for one-off composing.  (See
   [Other Useful Functions](http://www.hugsql.org/#using-other-fns))
 - Specify a function as private and other metadata (issue
   [#17](https://github.com/layerware/hugsql/issues/17))
 - Better support for Returning Execute and Insert w/ Return Keys (See
   [Insert Usage](http://www.hugsql.org/#using-insert)) (issues
   [#8](https://github.com/layerware/hugsql/issues/8) and
   [#15](https://github.com/layerware/hugsql/issues/15))
 - [Clojure Expressions](http://www.hugsql.org/#using-expressions)
 - [Snippets](http://www.hugsql.org/#using-snippets)
 - [Parameter Name Deep-Get](http://www.hugsql.org/#deep-get-param-name)
   for drilling down into parameter data
 - Better docs & tests
 
 
## 0.3.1

 - Fix on-exception bug w/ async (issue [#9](https://github.com/layerware/hugsql/issues/9))


## 0.2.x to 0.3.0

 - New doc site! [hugsql.org](http://www.hugsql.org)
 - [Comparison to Yesql](http://www.hugsql.org/#faq-yesql) (requested
   by many) (issue [#1](https://github.com/layerware/hugsql/issues/1))
 - [Princess Bride Example application](https://github.com/layerware/hugsql/tree/master/examples/princess-bride)
   as source for doc examples
 - [Tuple List Parameter Type](http://www.hugsql.org/#param-tuple-list) for multi-record insert support
 - [Pass-through options to the underlying database library](http://www.hugsql.org/#using-advanced) (e.g., :as-arrays?)
 - Defer adapter selection as late as possible (issue
   [#10](https://github.com/layerware/hugsql/issues/10))
 - Added on-exception to HugsqlAdapter protocol to allow
   implementations to redirect exceptions (helps with usage in
   core.async channels) (issue [#9](https://github.com/layerware/hugsql/issues/9))
 - Error checks for sql file existence, parameter mismatch errors
 - Minor bug fixes
