# HugSQL CHANGELOG

## 0.4.8
 - add validation exception for no value for :name (issue
   [#39](https://github.com/layerware/hugsql/issues/39))
 - add clojure.jdbc support for .getGeneratedKeys
 - stricter parsing for whitespace; (issue
   [#41](https://github.com/layerware/hugsql/issues/41))
 - fix truncated SQL when blank comment encountered (issue
   [#44](https://github.com/layerware/hugsql/issues/44))
 - add test profiles for clojure versions
 - fix multiarity macros definitions (Thanks to Joel Kaasinen)
   (pull request [#66](https://github.com/layerware/hugsql/pull/66))
 - doc fixes
 - bump version dependencies

## 0.4.7

 - fix deprecations for latest clojure.java.jdbc changes (issue
   [#38](https://github.com/layerware/hugsql/issues/38))

## 0.4.6

 - extend identifier param types to support SQL aliases (issue
   [#33](https://github.com/layerware/hugsql/issues/33))
 - fix clj expr eating trailing newline in parser (issue
   [#37](https://github.com/layerware/hugsql/issues/37))
 - fix command & result being ignored in private fns (issue
   [#32](https://github.com/layerware/hugsql/issues/32))

## 0.4.5

 - support pg double-colon type cast when suffix of HugSQL param (issue
   [#30](https://github.com/layerware/hugsql/issues/30))
 - doc: update escaping colon section to mention double-colon usage

## 0.4.4

 - fix parser error regarding whitespace (issue
   [#27](https://github.com/layerware/hugsql/issues/27))
 - fix handling of vector value parameters (issue
   [#28](https://github.com/layerware/hugsql/issues/28))
 - docs: add faq on preventing sql injection

## 0.4.3

 - report better error message for missing HugSQL header (issue
   [#24](https://github.com/layerware/hugsql/issues/24))
 - fix parser handling of Windows newlines (issue
   [#26](https://github.com/layerware/hugsql/issues/26))
 - missing parameter validation now checks
   [deep-get param names](http://www.hugsql.org/#deep-get-param-name)
 - add type hints for reflection warnings

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
