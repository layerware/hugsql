---
sidebar_position: 3
---

import { hugsqlVersion } from '@site/src/version.js'

# clojure.java.jdbc Adapter

Adapter for [clojure.java.jdbc](https://github.com/clojure/java.jdbc).

*Included here for reference, but this is unnecessary if you are using the `hugsql` meta clojar.*

### deps.edn

<pre><code>
{`com.layerware/hugsql-core {:mvn/version "${hugsqlVersion}"}
com.layerware/hugsql-adapter-clojure-java-jdbc {:mvn/version "${hugsqlVersion}"}`}
</code></pre>

### Leiningen

<pre><code>
{`[com.layerware/hugsql-core "${hugsqlVersion}"]
[com.layerware/hugsql-adapter-clojure-java-jdbc "${hugsqlVersion}"]`}
</code></pre>

See [Setting An Adapter](/hugsql-adapters/setting-an-adapter) for enabling an adapter in your code.
