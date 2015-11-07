(ns princess-bride.db.quotes
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "princess_bride/db/sql/quotes.sql")

(hugsql/def-sqlvec-fns "princess_bride/db/sql/quotes.sql")

