(ns hugsql.adapter.clojure-java-jdbc
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [clojure.java.jdbc :as jdbc]))

(deftype HugsqlAdapterClojureJavaJdbc []

  adapter/HugsqlAdapter
  (execute [_ db sqlvec options]
    (if (some #(= % (:command options)) [:insert :i!])
      (jdbc/db-do-prepared-return-keys db sqlvec)
      (apply jdbc/execute! db sqlvec (:command-options options))))

  (query [_ db sqlvec options]
    (apply jdbc/query db sqlvec (:command-options options)))

  (result-one [_ result _options]
    (first result))

  (result-many [_ result _options]
    result)

  (result-affected [_ result _options]
    (first result))

  (result-raw [_ result _options]
    result)

  (on-exception [_ exception]
    (throw exception)))

(defn hugsql-adapter-clojure-java-jdbc []
  (->HugsqlAdapterClojureJavaJdbc))
