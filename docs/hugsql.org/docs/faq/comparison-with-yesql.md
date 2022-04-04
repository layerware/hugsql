---
sidebar_position: 0
---

# Comparison with Yesql

Yesql is a Clojure library written by Kris Jenkins. It has a similar take on using SQL that HugSQL embraces whole-heartedly. Yesql is the spiritual predecessor to HugSQL, and HugSQL would not exist were it not for this great library.

**So why build a similar library?**

> A project of mine with somewhat complex SQL required me to generate dynamically-named tables and views with variable columns, and I found myself having to revert back to string concatenation for building up my SQL. I realized I needed something similar to Yesql, but with support for different types of parameter placeholders: namely, SQL Identifiers and SQL Keywords. This was the seed that grew into HugSQL, and the two libraries have quite a few differences now. --Curtis Summers

Differences between Yesql and HugSQL:

- Yesql is coupled to `clojure.java.jdbc`. HugSQL has protocol-based adapters to allow multiple database backend libraries and ships with support for `clojure.java.jdbc`, `next.jdbc`, and `clojure.jdbc`. This functionality has enabled [multiple adapters from the community](/hugsql-adapters/community-adapters). See [HugSQL Adapters](/hugsql-adapters) for more information.
- Yesql only supports SQL Value parameters. HugSQL supports [SQL Values](/hugsql-in-detail/parameter-types/sql-value-parameters), [SQL Tuples](/hugsql-in-detail/parameter-types/sql-tuple-parameters), [SQL Identifiers](/hugsql-in-detail/parameter-types/sql-identifier-parameters), [Raw SQL Parameters](/hugsql-in-detail/parameter-types/sql-raw-parameters), and creation of your own [custom parameter types](/hugsql-in-detail/parameter-types/custom-parameter-types). See [Parameter Types](/hugsql-in-detail/parameter-types/) for more detail.
- Yesql supports positional parameter placeholders `?` and named parameter placeholders `:id`. HugSQL only supports named parameter placeholders and there are no plans to support positional placeholders.
- Yesql tends to favor naming conventions of the function name (`!`, and `<!` suffixes) to indicate functionality. HugSQL prefers explicit configuration in the SQL file.
HugSQL features a `:result` configuration that indicates the expected return format (e.g., `:many` = vector of hashmaps, `:one` = hashmap). Yesql supports a similar functionality by passing the `:result-set-fn` option through to `clojure.java.jdbc/query`.
- Yesql (as of `0.5.x`) supports setting a default database connection at the time of function definition, and optionally overriding this connection at function call time. HugSQL expects a database spec, connection, or transaction object as the first argument to your function call. However, as of version `0.4.1`, HugSQL provides `map-of-db-fns` allowing other libraries to wrap HugSQL-created functions and set a default database connection. This is precisely what the Luminus web framework's [conman](https://github.com/luminus-framework/conman) library does.
- As of HugSQL `0.4.0`, HugSQL supports Clojure expressions and Snippets for composing SQL from smaller parts.
- Yesql is frozen with a maintainer sought as of version `0.5.3`.
