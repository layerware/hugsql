(ns hugsql.adapter.next-jdbc
  "next.jdbc adapter for HugSQL."
  (:gen-class)
  (:require [hugsql.adapter :as adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(deftype HugsqlAdapterNextJdbc [default-command-options]

  adapter/HugsqlAdapter
  (execute [_ db sqlvec {:keys [command-options]
                         :or   {command-options default-command-options}
                         :as   options}]
    (jdbc/execute! db sqlvec
                   (if (some #(= % (:command options)) [:insert :i!])
                     (assoc command-options :return-keys true)
                     command-options)))

  (query [_ db sqlvec options]
    (jdbc/execute! db sqlvec (or (:command-options options) default-command-options)))

  (result-one [_ result _options]
    (first result))

  (result-many [_ result _options]
    result)

  (result-affected [_ result _options]
    (:next.jdbc/update-count (first result)))

  (result-raw [_ result _options]
    result)

  (on-exception [_ exception]
    (throw exception)))

(defn hugsql-adapter-next-jdbc
  ([]
   (hugsql-adapter-next-jdbc {}))
  ([default-command-options]
   (->HugsqlAdapterNextJdbc (merge {:builder-fn rs/as-unqualified-lower-maps} default-command-options))))