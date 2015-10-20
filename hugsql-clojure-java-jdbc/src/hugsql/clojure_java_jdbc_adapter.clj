(ns hugsql.clojure-java-jdbc-adapter
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [clojure.java.jdbc :as jdbc]))

(deftype HugsqlClojureJavaJdbcAdapter []

  adapter/HugsqlAdapter
  (execute
    [this db sqlvec options]
    (jdbc/execute! db sqlvec))

  (query
    [this db sqlvec options]
    (jdbc/query db sqlvec))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    (first result))

  (result-raw [this result options]
    result))

(defn hugsql-clojure-java-jdbc-adapter []
  (->HugsqlClojureJavaJdbcAdapter))
