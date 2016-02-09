(ns hugsql.adapter.clojure-java-jdbc
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [clojure.java.jdbc :as jdbc]))

(deftype HugsqlAdapterClojureJavaJdbc []

  adapter/HugsqlAdapter
  (execute [this db sqlvec options]
    (if (some #(= % (:command options)) [:insert! :i!])
      (let [[sql & params] sqlvec]
        (jdbc/db-do-prepared-return-keys db sql params))
      (apply jdbc/execute! db sqlvec (:command-options options))))

  (query [this db sqlvec options]
    (apply jdbc/query db sqlvec (:command-options options)))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    (first result))

  (result-raw [this result options]
    result)

  (on-exception [this exception]
    (throw exception)))

(defn hugsql-adapter-clojure-java-jdbc []
  (->HugsqlAdapterClojureJavaJdbc))
