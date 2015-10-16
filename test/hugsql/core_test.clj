(ns hugsql.core-test
  (:require [clojure.test :refer :all]
            [hugsql.core :as hugsql]))

(def dbs
  {:postgresql  {:subprotocol "postgresql"
                 :subname "//127.0.0.1:5432/pgtest"
                 :user "pgtest"
                 :password "pgtest"}})

(hugsql/def-sql-fns "hugsql/sql/test.sql")
(hugsql/def-sql-str-fns "hugsql/sql/test.sql")

(doseq [[db-name db] dbs]
  
  (deftest core
    (testing "fn definition"
      (is (fn? no-params-select))
      (is (fn? no-params-select-sql))
      (is (= "No params" (:doc (meta #'no-params-select))))
      (is (= "No params (sql)" (:doc (meta #'no-params-select-sql)))))

    (testing "sql fns"
      (is (= ["select * from test"] (no-params-select-sql db)))
      (is (= ["select * from test"] (no-params-select-sql db {})))
      (is (= ["select * from test where id = ?" 1]
            (one-value-param-sql db {:id 1})))
      (is (= ["select * from test\nwhere id = ?\nand name = ?" 1 "Ed"]
            (multi-value-params-sql db {:id 1 :name "Ed"})))
      (is (= ["select * from test\nwhere id in (?,?,?)" 1 2 3]
            (value-list-param-sql db {:ids [1,2,3]})))
      (is (= ["select * from test"]
            (identifier-param-sql db {:table-name "test"}))))

    (testing "identifier quoting"
      (is (= ["select * from \"schema\".\"te\"\"st\""]
            (identifier-param-sql db {:table-name "schema.te\"st"} {:quote-identifiers :ansi})))
      (is (= ["select * from `schema`.`te``st`"]
            (identifier-param-sql db {:table-name "schema.te`st"} {:quote-identifiers :mysql})))
      (is (= ["select * from [schema].[te]]st]"]
            (identifier-param-sql db {:table-name "schema.te]st"} {:quote-identifiers :mssql}))))

    ))
