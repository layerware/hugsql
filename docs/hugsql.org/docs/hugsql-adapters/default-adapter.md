---
sidebar_position: 1
---

# Default Adapter

HugSQL defaults to using the adapter for the `clojure.java.jdbc` library. If you would prefer to use the adapter for `next.jdbc`, `clojure.jdbc`, or another adapter, you will need to configure your dependencies and set the adapter.

The `hugsql` clojar is a meta clojar that pulls in `hugsql-core`, `hugsql-adapter`, and the default adapter `hugsql-adapter-clojure-java-jdbc`, which uses `clojure.java.jdbc` to run database queries.

If you wish to use a different adapter, you should bypass the `hugsql` clojar and specify `hugsql-core` and the adapter clojar you desire.
