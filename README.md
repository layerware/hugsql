# HugSQL

![alt tag](https://raw.github.com/layerware/hugsql/master/logo/hugsql_alpha_128.png)

A Clojure library for embracing SQL.

## Documentation

Full documentation is at [hugsql.org](http://www.hugsql.org)

## API Documentation

[API Docs](http://layerware.github.io/hugsql)

## Recent Changes

## 0.4.1

 - map-of-db-fns, map-of-sqlvec-fns & -from-string variants for easier
   custom use of hugsql-created functions (issue
   [#19](https://github.com/layerware/hugsql/issues/19)) (See
   [Other Useful Functions](http://www.hugsql.org/#using-other-fns))
 - link to postgresql async adapter fixed (pull
   [#20](https://github.com/layerware/hugsql/pull/20))
 - minor test & doc updates

### 0.4.0

 - Escape colon in SQL (issue
   [#13](https://github.com/layerware/hugsql/issues/13))
 - def-db-fns-from-string & def-sql-fns-from-string (issue
   [#16](https://github.com/layerware/hugsql/issues/16))
 - Added several functions that operate on individual SQL statements,
   which are useful at the REPL or for one-off composing.  (See
   [Other Useful Functions](http://www.hugsql.org/#using-other-fns))
 - Specify a function as private and other metadata (issue [#17](https://github.com/layerware/hugsql/issues/17))
 - Better support for Returning Execute and Insert w/ Return Keys (See
   [Insert Usage](http://www.hugsql.org/#using-insert)) (issues
   [#8](https://github.com/layerware/hugsql/issues/8) and
   [#15](https://github.com/layerware/hugsql/issues/15))
 - [Clojure Expressions](http://www.hugsql.org/#using-expressions)
 - [Snippets](http://www.hugsql.org/#using-snippets)
 - [Parameter Name Deep-Get](http://www.hugsql.org/#deep-get-param-name) for drilling down into parameter data
 - Better docs & tests

[See CHANGELOG for more](https://github.com/layerware/hugsql/blob/master/CHANGELOG.md)


## License

Copyright © 2016 [Layerware, Inc.](http://www.layerware.com)

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
