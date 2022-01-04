---
sidebar_position: 1
---

import { hugsqlVersion } from '@site/src/version.js'

# Installation

## Dependencies

### deps.edn

<pre><code>
{`com.layerware/hugsql {:mvn/version "${hugsqlVersion}"}`}
</code></pre>

### Leiningen

<pre><code>
{`[com.layerware/hugsql "${hugsqlVersion}"]`}
</code></pre>

## JDBC Driver Dependencies

You will also need to specify your JDBC driver dependency from one of the following:

- [Apache Derby](https://search.maven.org/artifact/org.apache.derby/derby)
- [H2](https://search.maven.org/artifact/com.h2database/h2)
- [MS SQL Server](https://search.maven.org/artifact/com.microsoft.sqlserver/mssql-jdbc)
- [MySQL](https://search.maven.org/artifact/mysql/mysql-connector-java)
- [Oracle](https://search.maven.org/artifact/com.oracle.database.jdbc/ojdbc11)
- [Postgresql](https://search.maven.org/artifact/org.postgresql/postgresql)
- [SQLite](https://search.maven.org/artifact/org.xerial/sqlite-jdbc)

For example, to provide support for Postgresql:

### Postgresql JDBC deps.edn

```
org.postgresql/postgresql {:mvn/version "42.3.1"}
```

### Postgresql JDBC Leiningen

```
[org.postgresql/postgresql "42.3.1"]
```

:::info
HugSQL defaults to using the `clojure.java.jdbc` library to run underlying database commands. If you would prefer to use another underlying database library instead of `clojure.java.jdbc`, such as `next.jdbc`, please see [HugSQL Adapters](/hugsql-adapters).
:::
