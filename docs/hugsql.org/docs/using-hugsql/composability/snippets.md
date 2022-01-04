---
sidebar_position: 2
---

# Snippets

Snippets allow query composition by defining and generating portions of SQL statements and then using the Snippet Parameter Types to place these snippets into a full SQL statement.

You can define a snippet in your SQL file with `-- :snip my-snippet` (as opposed to `:name`):

```sql title="SQL"
-- :snip select-snip
select :i*:cols

-- :snip from-snip
from :i*:tables
```

Snippets can contain snippet parameters. Below, `:snip*:cond` is a Snippet List Parameter Type, which specifies that `:cond` is a list/vector of snippets

```sql title="SQL"
-- :snip where-snip
where :snip*:cond
```

Snippets can get very elaborate (creating an entire DSL) if you want. The cond-snip snippet below uses [deep-get parameter name](/hugsql-in-detail/parameter-types/deep-get-parameter-names) access to construct a where clause condition:

```sql title="SQL"
-- :snip cond-snip
-- We could come up with something
-- quite elaborate here with some custom
-- parameter types that convert := to =, etc.,
-- Examples:
-- {:conj "and" :cond ["id" "=" 1]}
-- OR
-- {:conj "or" :cond ["id" "=" 1]}
-- note that :conj can be "", too
:sql:conj :i:cond.0 :sql:cond.1 :v:cond.2
```

Using the above snippets, we can now construct the full query. (With an optional where clause via a Clojure expression)

```sql title="SQL"
-- :name snip-query :? :*
:snip:select
:snip:from
--~ (when (:where params) ":snip:where")
```

```clojure title="Clojure"
(snip-query
  db
  {:select (select-snip {:cols ["id","name"]})
   :from (from-snip {:tables ["test"]})
   :where (where-snip {:cond [(cond-snip {:conj "" :cond ["id" "=" 1]})
                              (cond-snip {:conj "or" :cond ["id" "=" 2]})]})})
```

:::note
It's worth noting that a snippet returns an sqlvec. This small detail gives you a great deal of flexibility in providing snippets to your HugSQL queries. Why? Because you don't necessarily need to create your own snippet DSL: you could use [another library](https://github.com/seancorfield/honeysql) for this. It is the best of both worlds! *This exercise is left to the reader.*
:::
