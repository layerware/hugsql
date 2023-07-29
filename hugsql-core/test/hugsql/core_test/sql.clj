(ns hugsql.core-test.sql
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "hugsql/sql/test.sql")
(hugsql/def-sqlvec-fns "hugsql/sql/test.sql")