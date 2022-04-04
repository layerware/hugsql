---
sidebar_position: 0
---

# Parameter Types

Within an SQL statement itself, HugSQL understands several types of parameters that can be passed in during the function call. All parameter types take the form:

```text
:param-name
```

OR

```text
:param-type:param-name
```

When a HugSQL-generated function is called, the parameters in an SQL statement are replaced at runtime with the hash-map data passed in as the function's second argument. The keys of the hash-map are matched to the `:param-name` portion of the parameter. Parameters can be repeated throughout an SQL statement, and all instances of a parameter will be replaced.
