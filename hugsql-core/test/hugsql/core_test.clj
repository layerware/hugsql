(ns hugsql.core-test
  (:require [clojure.test :refer :all]
            [hugsql.core :as hugsql]
            [hugsql.adapter.clojure-java-jdbc :as cjj-adapter]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]))

(def tmpdir (System/getProperty "java.io.tmpdir"))

(def dbs
  ;; sudo su - postgres ;; switch to postgres user
  ;; createuser -P hugtest ;; enter "hugtest" when prompted
  ;; createdb -O hugtest hugtest
  {:postgresql  {:subprotocol "postgresql"
                 :subname "//127.0.0.1:5432/hugtest"
                 :user "hugtest"
                 :password "hugtest"}
   

   ;; mysql -u root -p
   ;; mysql> create database hugtest;
   ;; mysql> grant all on hugtest.* to hugtest identified by "hugtest";
   :mysql  {:subprotocol "mysql"
            :subname "//127.0.0.1:3306/hugtest"
            :user "hugtest"
            :password "hugtest"}

   :sqlite {:subprotocol "sqlite"
            :subname (str tmpdir "/hugtest.sqlite")}                          
   
   :h2 {:subprotocol "h2"
        :subname (str tmpdir "/hugtest.h2")}

   :hsqldb {:dbtype "hsqldb"
            :dbname (str tmpdir "/hugtest.hsqldb")}

   :derby {:dbtype "derby"
           :dbname (str tmpdir "/hugtest.derby")
           :create true}})

(hugsql/def-db-fns "hugsql/sql/test.sql")
(hugsql/def-sqlvec-fns "hugsql/sql/test.sql")

(deftest core
  (doseq [[db-name db] dbs]
    
    (testing "fn definition"
      (is (fn? no-params-select))
      (is (fn? no-params-select-sqlvec))
      (is (= "No params" (:doc (meta #'no-params-select))))
      (is (= "No params (sqlvec)" (:doc (meta #'no-params-select-sqlvec)))))

    (testing "sql fns"
      (is (= ["select * from test"] (no-params-select-sqlvec db)))
      (is (= ["select * from test"] (no-params-select-sqlvec db {})))
      (is (= ["select * from test where id = ?" 1]
            (one-value-param-sqlvec db {:id 1})))
      (is (= ["select * from test\nwhere id = ?\nand name = ?" 1 "Ed"]
            (multi-value-params-sqlvec db {:id 1 :name "Ed"})))
      (is (= ["select * from test\nwhere id in (?,?,?)" 1 2 3]
            (value-list-param-sqlvec db {:ids [1,2,3]})))
      (is (= ["select * from test"]
            (identifier-param-sqlvec db {:table-name "test"})))
      (is (= ["select id, name from test"]
            (identifier-param-list-sqlvec db {:columns ["id", "name"]})))
      (is (= ["select * from test order by id desc"]
            (sql-param-sqlvec db {:id-order "desc"}))))

    (testing "identifier quoting"
      (is (= ["select * from \"schema\".\"te\"\"st\""]
            (identifier-param-sqlvec db
              {:table-name "schema.te\"st"}
              {:quoting :ansi})))
      (is (= ["select * from `schema`.`te``st`"]
            (identifier-param-sqlvec db
              {:table-name "schema.te`st"}
              {:quoting :mysql})))
      (is (= ["select * from [schema].[te]]st]"]
            (identifier-param-sqlvec db
              {:table-name "schema.te]st"}
              {:quoting :mssql})))
      (is (= ["select \"test\".\"id\", \"test\".\"name\" from test"]
            (identifier-param-list-sqlvec db
              {:columns ["test.id", "test.name"]}
              {:quoting :ansi})))
      (is (= ["select `test`.`id`, `test`.`name` from test"]
            (identifier-param-list-sqlvec db
              {:columns ["test.id", "test.name"]}
              {:quoting :mysql})))
      (is (= ["select [test].[id], [test].[name] from test"]
            (identifier-param-list-sqlvec db
              {:columns ["test.id", "test.name"]}
              {:quoting :mssql}))))

    (testing "adapter"
      (is (= hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc
            (type (hugsql/get-adapter))))
      (hugsql/set-adapter! (cj-adapter/hugsql-adapter-clojure-jdbc))
      (is (= hugsql.adapter.clojure_jdbc.HugsqlAdapterClojureJdbc
            (type (hugsql/get-adapter))))
      (hugsql/set-adapter! nil)
      (is (= hugsql.adapter.clojure_java_jdbc.HugsqlAdapterClojureJavaJdbc
            (type (hugsql/get-adapter)))))

    (testing "command: execute"
      (is (= 0 (create-test-table db)))
      (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
      (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))
      (is (= 1 (update-test-table db {:id 1 :name "C"})))
      (is (= {:id 1 :name "C"} (select-one-test-by-id db {:id 1})))
      (is (= 0 (drop-test-table db))))
    
    (testing "query-type: query"
      (create-test-table db)
      
      (drop-test-table db)
     )
    ))

