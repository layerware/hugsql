(ns hugsql.core-test
  (:require [clojure.test :refer :all]
            [hugsql.core :as hugsql]
            [hugsql.adapter]
            [hugsql.adapter.clojure-java-jdbc :as cjj-adapter]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [clojure.java.io :as io])
  (:import [clojure.lang ExceptionInfo]))

(def adapters
  {:clojure.java.jdbc (cjj-adapter/hugsql-adapter-clojure-java-jdbc)
   :clojure.jdbc (cj-adapter/hugsql-adapter-clojure-jdbc)})

(def tmpdir (System/getProperty "java.io.tmpdir"))

(def dbs {
  ;; sudo su - postgres ;; switch to postgres user
  ;; createuser -P hugtest ;; enter "hugtest" when prompted
  ;; createdb -O hugtest hugtest
  :postgresql  {:subprotocol "postgresql"
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

   :hsqldb {:subprotocol "hsqldb"
            :subname (str tmpdir "/hugtest.hsqldb")}

   :derby {:subprotocol "derby"
           :subname (str tmpdir "/hugtest.derby")
           :create true}
})

;; Call def-db-fns outside of deftest so that namespace is 'hugsql.core-test
;; Use a file path in the classpath
(hugsql/def-db-fns "hugsql/sql/test.sql")
(hugsql/def-sqlvec-fns "hugsql/sql/test.sql")

;; Use a java.io.File object
(let [tmpfile (io/file (str tmpdir "/test.sql"))]
      (io/copy (io/file (io/resource "hugsql/sql/test2.sql")) tmpfile)
      (hugsql/def-db-fns tmpfile))

;; Use a string
(hugsql/def-db-fns-from-string "-- :name test3-select\n select * from test3")

(deftest core

  (testing "adapter was not set during fn def"
    (is (= nil hugsql/adapter)))

  (testing "File outside of classpath with java.io.File worked"
    (is (fn? test2-select)))

  (testing "defs from string worked"
    (is (fn? test3-select)))

  (testing "sql file does not exist/can't be read"
    (is (thrown-with-msg? ExceptionInfo #"Can not read file"
          (eval '(hugsql.core/def-db-fns "non/existent/file.sql")))))
  
  (testing "fn definition"
    (is (fn? no-params-select))
    (is (fn? no-params-select-sqlvec))
    (is (= "No params" (:doc (meta #'no-params-select))))
    (is (= "No params (sqlvec)" (:doc (meta #'no-params-select-sqlvec)))))

  (testing "sql fns"
    (is (= ["select * from test"] (no-params-select-sqlvec)))
    (is (= ["select * from test"] (no-params-select-sqlvec {})))
    (is (= ["select * from test where id = ?" 1]
          (one-value-param-sqlvec {:id 1})))
    (is (= ["select * from test\nwhere id = ?\nand name = ?" 1 "Ed"]
          (multi-value-params-sqlvec {:id 1 :name "Ed"})))
    (is (= ["select * from test\nwhere id in ( ?,?,? )" 1 2 3]
          (value-list-param-sqlvec {:ids [1,2,3]})))
    (is (= ["select * from test\nwhere (id, name) = (?,?)" 1 "A"]
          (tuple-param-sqlvec {:id-name [1 "A"]})))
    (is (= ["insert into test (id, name)\nvalues (?,?),(?,?),(?,?)" 1 "Ed" 2 "Al" 3 "Bo"]
          (tuple-param-list-sqlvec {:people [[1 "Ed"] [2 "Al"] [3 "Bo"]]})))
    (is (= ["select * from test"]
          (identifier-param-sqlvec {:table-name "test"})))
    (is (= ["select id, name from test"]
          (identifier-param-list-sqlvec {:columns ["id", "name"]})))
    (is (= ["select * from test order by id desc"]
          (sql-param-sqlvec {:id-order "desc"}))))

  (testing "identifier quoting"
    (is (= ["select * from \"schema\".\"te\"\"st\""]
          (identifier-param-sqlvec
            {:table-name "schema.te\"st"}
            {:quoting :ansi})))
    (is (= ["select * from `schema`.`te``st`"]
          (identifier-param-sqlvec
            {:table-name "schema.te`st"}
            {:quoting :mysql})))
    (is (= ["select * from [schema].[te]]st]"]
          (identifier-param-sqlvec
            {:table-name "schema.te]st"}
            {:quoting :mssql})))
    (is (= ["select \"test\".\"id\", \"test\".\"name\" from test"]
          (identifier-param-list-sqlvec
            {:columns ["test.id", "test.name"]}
            {:quoting :ansi})))
    (is (= ["select `test`.`id`, `test`.`name` from test"]
          (identifier-param-list-sqlvec
            {:columns ["test.id", "test.name"]}
            {:quoting :mysql})))
    (is (= ["select [test].[id], [test].[name] from test"]
          (identifier-param-list-sqlvec
            {:columns ["test.id", "test.name"]}
            {:quoting :mssql}))))

  (testing "sqlvec"
    (is (= ["select * from test where id = ?" 1]
           (hugsql/sqlvec "select * from test where id = :id" {:id 1}))))

  (testing "metadata"
    (is (:private (meta #'a-private-fn)))
    (is (:private (meta #'another-private-fn)))
    (is (= 1 (:one (meta #'user-meta))))
    (is (= 2 (:two (meta #'user-meta)))))
  
  (doseq [[db-name db] dbs]
    (doseq [[adapter-name adapter] adapters]

      (testing "adapter set"
        (is (satisfies? hugsql.adapter/HugsqlAdapter (hugsql/set-adapter! adapter))))

      (testing "parameter placeholder vs data mismatch"
        (is (thrown-with-msg? ExceptionInfo
                              #"Parameter Mismatch: :id parameter data not found."
              (one-value-param-sqlvec {:x 1})))
        (is (thrown-with-msg? ExceptionInfo
                              #"Parameter Mismatch: :id parameter data not found."
              (one-value-param db {:x 1}))))

      (testing "database commands/queries"
        (condp = db-name
          :mysql (is (= 0 (create-test-table-mysql db)))
          :h2 (is (= 0 (create-test-table-h2 db)))
          :hsqldb (is (= 0 (create-test-table-hsqldb db)))
          (is (= 0 (create-test-table db))))
        (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
        (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))

        ;; tuple use is not supported by certain dbs
        (when (not-any? #(= % db-name) [:derby :sqlite :hsqldb])
          (is (= [{:id 1 :name "A"}] (tuple-param db {:id-name [1 "A"]}))))

        ;; only hsqldb appears to not support multi-insert values for :tuple*
        (when (not (= db-name :hsqldb))
          (is (= 3 (insert-multi-into-test-table db {:values [[4 "D"] [5 "E"] [6 "F"]]}))))

        ;; returning support is lacking in many dbs
        (when (not-any? #(= % db-name) [:mysql :h2 :derby :sqlite :hsqldb])
          (is (= [{:id 7}]
                 (insert-into-test-table-returning db {:id 7 :name "G"}))))

        ;; return generated keys, which has varying support and return values
        (when (= adapter-name :clojure.java.jdbc)
          (condp = db-name
            :postgresql
            (is (= {:id 8 :name "H"}
                   (insert-into-test-table-return-keys db {:id 8 :name "H"} {})))

            :mysql
            (is (= {:generated_key 9} (insert-into-test-table-return-keys db {:id 9 :name "I"})))

            :sqlite
            (is (= {(keyword "last_insert_rowid()") 10}
                   (insert-into-test-table-return-keys db {:id 10 :name "J"} {})))
            :h2
            (is (= {(keyword "scope_identity()") 11}
                   (insert-into-test-table-return-keys db {:id 11 :name "J"} {})))

            ;; hsql and derby don't seem to support .getGeneratedKeys
            nil))

        (is (= 1 (update-test-table db {:id 1 :name "C"})))
        (is (= {:id 1 :name "C"} (select-one-test-by-id db {:id 1})))
        (is (= {:id 1 :name "C"} (select-deep-get db {:records [{:id 1}]})))
        (is (= 0 (drop-test-table db))))

      (testing "db-fn"
        (is (= 0 (create-test-table db)))
        (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
        (is (fn? (hugsql/db-fn "select * from test where id = :id" :? :1)))
        (is (= "A" (:name
                    (let [f (hugsql/db-fn "select * from test where id = :id" :? :1)]
                      (f db {:id 1})))))
        (is (= 0 (drop-test-table db))))

      (testing "db-run"
        (is (= 0 (create-test-table db)))
        (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
        (is (= "A" (:name
                    (hugsql/db-run db "select * from test where id = :id" {:id 1} :? :1))))
        (is (= 0 (drop-test-table db))))

      (testing "adapter-specific command option pass-through"
        (is (= 0 (create-test-table db)))
        (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
        (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))

        (when (= adapter-name :clojure.java.jdbc)
          (is (= [[:name] ["A"] ["B"]]
                (select-ordered db
                  {:cols ["name"] :sort-by ["name"]} {} :as-arrays? true))))

        (is (= 0 (drop-test-table db))))

      (testing "Clojure expressions"
        (is (= 0 (create-test-table db)))
        (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
        (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))
        (is (= 1 (insert-into-test-table db {:id 3 :name "C"})))

        (is (= {:name "A"} (clj-expr-single db {:cols ["name"]})))
        (is (= {:id 1 :name "A"} (clj-expr-single db)))

        (is (= {:name "A"} (clj-expr-multi db {:cols ["name"]})))
        (is (= {:id 1 :name "A"} (clj-expr-multi db)))

        (is (= {:id 1 :name "A"} (clj-expr-multi-when db)))
        (is (= {:id 2 :name "B"} (clj-expr-multi-when db {:id 2})))

        (is (= 1 (clj-expr-generic-update db {:table "test"
                                              :updates {:name "X"}
                                              :id 3})))
        (is (= 0 (drop-test-table db)))))))
