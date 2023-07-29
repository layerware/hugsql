(ns hugsql.adapter.clojure-jdbc
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [jdbc.core :as jdbc]))

(deftype HugsqlAdapterClojureJdbc []

  adapter/HugsqlAdapter
  (execute [_ db sqlvec options]
    (jdbc/execute db sqlvec
                  (merge {:returning (some #(= % (:command options)) [:insert :i!])}
                         (:command-options options))))

  (query [_ db sqlvec options]
    (jdbc/fetch db sqlvec (:command-options options)))

  (result-one [_ result _options]
    (first result))

  (result-many [_ result _options]
    result)

  (result-affected [_ result _options]
    result)

  (result-raw [_ result _options]
    result)

  (on-exception [_ exception]
    (throw exception)))

(defn hugsql-adapter-clojure-jdbc []
  (->HugsqlAdapterClojureJdbc))
