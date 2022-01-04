---
sidebar_position: 3
---

# Preventing SQL Injection

**How does HugSQL help protect against SQL injection?**

HugSQL attempts to provide a set of tools that help protect against SQL injection where possible without taking away power from the developer. Below are a few potential SQL injection attack vectors and HugSQL's response to each:

## Value Parameters

[Value Parameters](/hugsql-in-detail/parameter-types/sql-value-parameters), [Value List Parameters](/hugsql-in-detail/parameter-types/sql-value-list-parameters), [Tuple Parameters](/hugsql-in-detail/parameter-types/sql-tuple-parameters), and [Tuple List Parameters](/hugsql-in-detail/parameter-types/sql-tuple-list-parameters) are all variations on SQL value parameters that convert a Clojure data type to SQL. By default, all of these parameter types defer to the underlying database library to perform [SQL parameter binding](http://martinfowler.com/articles/web-security-basics.html#ParameterBindingToTheRescue) to prevent SQL injection issues.

## Identifier Parameters

[Identifier Parameters](/hugsql-in-detail/parameter-types/sql-identifier-parameters) and [Identifier List Parameters\(/hugsql-in-detail/parameter-types/sql-identifier-list-parameters) support quoting and escaping of identifiers with the `:quoting option`. By default, `:quoting` is `:off`, since HugSQL makes no assumptions about your given database. This may be fine for your use case if you are not taking identifiers from user input.

:::caution

If you are taking identifiers from user input, you should use the `:quoting` option to prevent SQL injection! See [Identifier Parameters](/hugsql-in-detail/parameter-types/sql-identifier-parameters) for details.

:::

## Raw SQL Parameters

:::danger

[Raw SQL Parameters](/hugsql-in-detail/parameter-types/sql-raw-parameters) are exactly what they seem, and it is your responsibility to sanitize any usage of this parameter type when using user input.

:::

## Snippet Parameters

[Snippets](/using-hugsql/composability/snippets) generate sqlvecs and Snippet Parameter Types consume sqlvecs. For snippets containing any HugSQL parameter types, the same rules as above apply. If you are consuming a snippet (or sqlvec) from your own code or another library (say, HoneySQL), then other rules might apply.

## Custom Parameter Types

[Custom Parameter Types](/hugsql-in-detail/parameter-types/custom-parameter-types) allow you to create your own parameter types. It is your responsibility to ensure your implementation protects against SQL injection by properly escaping your data.

## Clojure Expressions

[Clojure Expressions](/using-hugsql/composability/clojure-expressions) should return either a string or nil, and strings returned from expressions are parsed at runtime to support HugSQL parameters. The same rules apply for the above parameter types.
