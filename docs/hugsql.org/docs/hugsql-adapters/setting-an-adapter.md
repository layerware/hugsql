---
sidebar_position: 5
---

import { hugsqlVersion } from '@site/src/version.js'

# Setting an Adapter

Within your Clojure code, you will need to explicitly set the adapter. You can do this globally (i.e., at app startup) with `hugsql.core/set-adapter!`, or you can specify the `:adapter` as an option when defining your functions with `hugsql.core/def-db-fns`, or you can pass in an `:adapter` option when calling your defined function.

```clojure title="hugsql.core/set-adapter!"
(ns my-app
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter
            [next.jdbc.result-set :as rs]]]))

(defn app-init []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

;; OR override the :builder-fn behavior
(defn app-init []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc {:builder-fn result-set/as-unqualified-maps})))
```

OR

```clojure title=":adapter option on hugsql.core/def-db-fns"
(ns my-db-stuff
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

(hugsql/def-db-fns "path/to/db.sql"
  {:adapter (next-adapter/hugsql-adapter-next-jdbc)})
```

OR

```clojure title=":adapter option on defined function"
(ns my-db-stuff
(:require [hugsql.core :as hugsql]
          [hugsql.adapter.next-jdbc :as next-adapter]))

(def db ;;a db-spec here)

(hugsql/def-db-fns "path/to/db.sql")

(my-query db {:id 1}
  {:adapter (next-adapter/hugsql-adapter-next-jdbc)})
```
