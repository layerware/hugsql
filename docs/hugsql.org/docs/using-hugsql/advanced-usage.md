---
sidebar_position: 11
---

# Advanced Usage

Each underlying database library and corresponding HugSQL adapter may support additional options for the execute/query commands. Functions defined by `def-db-fns` have a variable-arity 4th argument that passes any options through to the underlying database library.

Below is an assertion from the HugSQL test suite showing a query passing along the `:as-arrays?` option to `clojure.java.jdbc/query`. Please note the required 3rd argument (the HugSQL-specific options) when using this passthrough feature:

```clojure title="Clojure"
(is (= [[:name] ["A"] ["B"]]
      (select-ordered db
        {:cols ["name"] :sort-by ["name"]} {} {:as-arrays? true})))
```

Please note that as of `clojure.java.jdbc` `0.5.8` and HugSQL `0.4.7`, the above additional options are now required to be a hashmap instead of keyword arguments as in previous versions. In `clojure.java.jdbc` `0.5.8` the deprecated usage will emit a warning. In `clojure.java.jdbc` `0.6.0+` the usage is deprecated and not allowed. See the [clojure.java.jdbc changelog](https://github.com/clojure/java.jdbc/blob/master/CHANGES.md) for details.
