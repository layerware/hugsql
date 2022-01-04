---
sidebar_position: 10
---

# Custom Parameter Types

You can create your own parameter types by implementing a method for the multimethod `hugsql.parameters/hugsql-apply-param`.:

```clojure
=> (doc hugsql.parameters/apply-hugsql-param)
-------------------------
hugsql.parameters/apply-hugsql-param
  Implementations of this multimethod apply a hugsql parameter
   for a specified parameter type.  For example:

   (defmethod apply-hugsql-param :value
     [param data options]
     (value-param param data options)

   - :value keyword is the parameter type to match on.
   - param is the parameter map as parsed from SQL
     (e.g., {:type :value :name "id"} )
   - data is the runtime parameter map data to be applied
     (e.g., {:id 42} )
   - options contain hugsql options (see hugsql.core/def-sqlvec-fns)

   Implementations must return a vector containing any resulting SQL
   in the first position and any values in the remaining positions.
   (e.g., ["?" 42])
```
