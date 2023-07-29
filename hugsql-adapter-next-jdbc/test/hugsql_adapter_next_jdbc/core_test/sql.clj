(ns hugsql-adapter-next-jdbc.core-test.sql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "hugsql_adapter_next_jdbc/core_test/queries.sql")
