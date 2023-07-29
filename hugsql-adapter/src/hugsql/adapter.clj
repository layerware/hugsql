(ns hugsql.adapter)

(defprotocol HugsqlAdapter
  "Hugsql Adapter Protocol"

  (execute [_ db sqlvec options]
    "Execute SQL/DDL/DML statement")

  (query [_ db sqlvec options]
    "Query SQL statement (expecting result set)")

  (result-one [_ result options]
    "Result: returns one hashmap record")

  (result-many [_ result options]
    "Result: returns vector of hashmap records")

  (result-affected [_ result options]
    "Result: returns integer of affected rows/records")

  (result-raw [_ result options]
    "Result: returns raw result, untouched")

  (on-exception [_ exception]
    "Exception handler allows adapter to redirect exceptions from HugSQL"))
