(ns hugsql.adapter.next-jdbc
  "next.jdbc adapter for HugSQL."
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(deftype HugsqlAdapterNextJdbc [default-command-options]

  adapter/HugsqlAdapter
  (execute [this db sqlvec {:keys [command-options]
                            :or   {command-options default-command-options}
                            :as   options}]
    (jdbc/execute! db sqlvec
                   (if (some #(= % (:command options)) [:insert :i!])
                     (assoc command-options :return-keys true)
                     command-options)))

  (query [this db sqlvec options]
    (jdbc/execute! db sqlvec (or (:command-options options) default-command-options)))

  (result-one [this result options]
    (first result))

  (result-many [this result options]
    result)

  (result-affected [this result options]
    (:next.jdbc/update-count (first result)))

  (result-raw [this result options]
    result)

  (on-exception [this exception]
    (throw exception)))

(defn hugsql-adapter-next-jdbc
  ([]
   (hugsql-adapter-next-jdbc {}))
  ([default-command-options]
   (->HugsqlAdapterNextJdbc (merge {:builder-fn rs/as-unqualified-lower-maps} default-command-options))))