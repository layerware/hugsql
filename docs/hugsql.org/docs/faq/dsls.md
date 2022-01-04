---
sidebar_position: 2
---

# What about DSLs for SQL?

Can I get the same SQL generation similar to [Honey SQL](https://github.com/seancorfield/honeysql)?

HugSQL has several options for composing SQL.  See [Composability](../using-hugsql/composability/).

HugSQL encourages you to think in SQL first, then sprinkle in the power of Clojure where necessary. HoneySQL starts on the Clojure side first. Both are valid workflows and a matter of developer preference and situation. It's important to realize that HugSQL and HoneySQL are not mutually exclusive: HugSQL Snippet Parameter Types `:snip` and `:snip*` can consume the sqlvec format output from HoneySQL's format function. It's the best of both worlds!
