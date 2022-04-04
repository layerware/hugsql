---
sidebar_position: 4
---

import { hugsqlVersion } from '@site/src/version.js'

# clojure.jdbc Adapter

Adapter for [clojure.jdbc](https://github.com/funcool/clojure.jdbc).

### deps.edn

<pre><code>
{`com.layerware/hugsql-core {:mvn/version "${hugsqlVersion}"}
com.layerware/hugsql-adapter-clojure-jdbc {:mvn/version "${hugsqlVersion}"}`}
</code></pre>

### Leiningen

<pre><code>
{`[com.layerware/hugsql-core "${hugsqlVersion}"]
[com.layerware/hugsql-adapter-clojure-jdbc "${hugsqlVersion}"]`}
</code></pre>

See [Setting An Adapter](/hugsql-adapters/setting-an-adapter) for enabling an adapter in your code.
