# HugSQL Adapter for next.jdbc

Special thanks to:

- [@nikolap](https://github.com/nikolap) for the original implementation
- [@seancorfield](https://github.com/seancorfield) for [next.jdbc](https://cljdoc.org/d/seancorfield/next.jdbc/)

## Usage

Below is an example, you can find other ways to do this in the [HugSQL documentation](https://www.hugsql.org/#adapter-other).

```clojure
(ns my-app
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

(defn app-init []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))
```

You can pass in default command options to the adapter via the one-arity version. For example:

```clojure
(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc {:builder-fn result-set/as-unqualified-maps}))
```

