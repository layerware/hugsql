---
sidebar_position: 2
---

import { hugsqlVersion } from '@site/src/version.js'

# next.jdbc Adapter

Adapter for [next.jdbc](https://github.com/seancorfield/next-jdbc). (as of HugSQL `0.5.1`).

In order to maintain the closest behavior to `clojure.java.jdbc`, the adapter currently defaults to setting the result set `:builder-fn` option to `next.jdbc.result-set/as-unqualified-lower-maps`. This behavior can be overridden when setting the adapter.

### deps.edn

<pre><code>
{`com.layerware/hugsql-core {:mvn/version "${hugsqlVersion}"}
com.layerware/hugsql-adapter-next-jdbc {:mvn/version "${hugsqlVersion}"}`}
</code></pre>

### Leiningen

<pre><code>
{`[com.layerware/hugsql-core "${hugsqlVersion}"]
[com.layerware/hugsql-adapter-next-jdbc "${hugsqlVersion}"]`}
</code></pre>

See [Setting An Adapter](/hugsql-adapters/setting-an-adapter) for enabling an adapter in your code.
