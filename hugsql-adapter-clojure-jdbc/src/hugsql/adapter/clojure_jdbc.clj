(ns hugsql.adapter.clojure-jdbc
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [jdbc.core :as jdbc]))

(deftype HugsqlAdapterClojureJdbc []

  adapter/HugsqlAdapter
  (execute
    [this db sqlvec options]
    (jdbc/execute db sqlvec))

  (query
    [this db sqlvec options]
    (jdbc/fetch db sqlvec))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    result)

  (result-raw [this result options]
    result))

(defn hugsql-adapter-clojure-jdbc []
  (->HugsqlAdapterClojureJdbc))
