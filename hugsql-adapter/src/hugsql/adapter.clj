(ns hugsql.adapter)

(defprotocol HugsqlAdapter
  "Hugsql Adapter Protocol"

  (execute [this db sqlvec options]
    "Execute SQL/DDL/DML statement")

  (query [this db sqlvec options]
    "Query SQL statement (expecting result set)")

  (result-one [this result options]
    "Result: returns one hashmap record")

  (result-many [this result options]
    "Result: returns vector of hashmap records")

  (result-affected [this result options]
    "Result: returns integer of affected rows/records")

  (result-raw [this result options]
    "Result: returns raw result, untouched")

  (on-exception [this exception]
    "Exception handler allows adapter to redirect exceptions from HugSQL"))
