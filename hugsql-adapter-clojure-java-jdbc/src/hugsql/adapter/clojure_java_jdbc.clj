(ns hugsql.adapter.clojure-java-jdbc
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [clojure.java.jdbc :as jdbc]))

(deftype HugsqlAdapterClojureJavaJdbc []

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

(defn hugsql-adapter-clojure-java-jdbc []
  (->HugsqlAdapterClojureJavaJdbc))
