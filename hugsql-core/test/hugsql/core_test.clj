(ns hugsql.core-test
  (:require [clojure.test :refer :all]
            [hugsql.core :as hugsql]
            [hugsql.adapter]
            [hugsql.adapter.clojure-java-jdbc :as cjj-adapter]
            [hugsql.adapter.clojure-jdbc :as cj-adapter]
            [hugsql.adapter.next-jdbc :as nj-adapter]
            [clojure.java.io :as io])
  (:import [clojure.lang ExceptionInfo]))

(def adapters
  {:clojure.java.jdbc (cjj-adapter/hugsql-adapter-clojure-java-jdbc)
   :clojure.jdbc (cj-adapter/hugsql-adapter-clojure-jdbc)
   :next.jdbc (nj-adapter/hugsql-adapter-next-jdbc)})

(def tmpdir (System/getProperty "java.io.tmpdir"))

(def dbs {;; sudo su - postgres ;; switch to postgres user
          ;; createuser -P hugtest ;; enter "hugtest" when prompted
          ;; createdb -O hugtest hugtest
          :postgresql  {:next.jdbc {:dbtype "postgresql"
                                    :dbname "hugtest"
                                    :host "127.0.0.1"
                                    :port "5432"
                                    :user "hugtest"
                                    :password "hugtest"}
                        :default {:subprotocol "postgresql"
                                  :subname "//127.0.0.1:5432/hugtest"
                                  :user "hugtest"
                                  :password "hugtest"}}

          ;; mysql -u root -p
          ;; mysql> create database hugtest;
          ;; mysql> grant all on hugtest.* to hugtest identified by "hugtest";
          :mysql  {:next.jdbc {:dbtype "mysql"
                               :dbname "hugtest?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"
                               :host "127.0.0.1"
                               :port "3306"
                               :user "hugtest"
                               :password "hugtest"}
                   :default {:subprotocol "mysql"
                             :classname "com.mysql.cj.jdbc.Driver"
                             :subname "//127.0.0.1:3306/hugtest?useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC"
                             :user "hugtest"
                             :password "hugtest"}}
          :sqlite {:next.jdbc {:dbtype "sqlite"
                               :dbname (str tmpdir "/hugtest.next.sqlite")}
                   :default {:subprotocol "sqlite"
                             :subname (str tmpdir "/hugtest.default.sqlite")}}

          :h2 {:next.jdbc {:dbtype "h2"
                           :dbname (str tmpdir "/hugtest.h2")}
               :default {:subprotocol "h2"
                         :subname (str tmpdir "/hugtest.h2")}}

          :hsqldb {:next.jdbc {:dbtype "hsqldb"
                               :dbname (str tmpdir "/hugtest.next.hsqldb")}
                   :default {:subprotocol "hsqldb"
                             :subname (str tmpdir "/hugtest.default.hsqldb")}}

          :derby {:next.jdbc {:dbtype "derby"
                              :dbname (str tmpdir "/hugtest.next.derby")
                              :create true}
                  :default {:subprotocol "derby"
                            :subname (str tmpdir "/hugtest.default.derby")
                            :create true}}})

;; Call def-db-fns outside of deftest so that namespace is 'hugsql.core-test
;; Use a file path in the classpath


(hugsql/def-db-fns "hugsql/sql/test.sql")
(hugsql/def-sqlvec-fns "hugsql/sql/test.sql")

;; Use a java.io.File object
(let [tmpfile (io/file (str tmpdir "/test.sql"))]
  (io/copy (io/file (io/resource "hugsql/sql/test2.sql")) tmpfile)
  (hugsql/def-db-fns tmpfile))

;; Use a string
(def hugsql-string-defs
  (str "-- :name test3-select\n select * from test3"
       "-- :snip snip1\n select *"))
(hugsql/def-db-fns-from-string hugsql-string-defs)

(deftest core

  (testing "File outside of classpath with java.io.File worked"
    (is (fn? test2-select)))

  (testing "defs from string worked"
    (is (fn? test3-select))
    (is (fn? snip1)))

  (testing "sql file does not exist/can't be read"
    (is (thrown-with-msg? ExceptionInfo #"Can not read file"
                          (hugsql.core/def-db-fns "non/existent/file.sql"))))

  (testing "fn definition"
    (is (fn? no-params-select))
    (is (fn? no-params-select-sqlvec))
    (is (= "No params" (:doc (meta #'no-params-select))))
    (is (= "No params (sqlvec)" (:doc (meta #'no-params-select-sqlvec))))
    (is (= "hugsql/sql/test.sql" (:file (meta #'no-params-select))))
    (is (= 6 (:line (meta #'no-params-select))))
    (is (= '([db]
             [db params]
             [db params options & command-options])
           (:arglists (meta #'no-params-select))))
    (is (= '([]
             [params]
             [params options])
           (:arglists (meta #'no-params-select-sqlvec)))))

  (testing "sql fns"
    (is (= ["select * from test"] (no-params-select-sqlvec)))
    (is (= ["select * from test"] (no-params-select-sqlvec {})))
    (is (= ["select * from test where id = ?" 1]
           (one-value-param-sqlvec {:id 1})))
    (is (= ["select * from test\nwhere id = ?\nand name = ?" 1 "Ed"]
           (multi-value-params-sqlvec {:id 1 :name "Ed"})))
    (is (= ["select * from test\nwhere id in (?,?,?)" 1 2 3]
           (value-list-param-sqlvec {:ids [1,2,3]})))
    (is (= ["select * from test\nwhere (id, name) = (?,?)" 1 "A"]
           (tuple-param-sqlvec {:id-name [1 "A"]})))
    (is (= ["insert into test (id, name)\nvalues (?,?),(?,?),(?,?)" 1 "Ed" 2 "Al" 3 "Bo"]
           (tuple-param-list-sqlvec {:people [[1 "Ed"] [2 "Al"] [3 "Bo"]]})))
    (is (= ["select * from test"]
           (identifier-param-sqlvec {:table-name "test"})))
    (is (= ["select id, name from test"]
           (identifier-param-list-sqlvec {:columns ["id", "name"]})))
    (is (= ["select * from test as my_test"]
           (identifier-param-sqlvec {:table-name ["test" "my_test"]})))
    (is (= ["select id as my_id, name as my_name from test"]
           (identifier-param-list-sqlvec {:columns [["id" "my_id"], ["name" "my_name"]]})))
    (is (= ["select * from test as my_test"]
           (identifier-param-sqlvec {:table-name {"test" "my_test"}})))
    (is (let [r (identifier-param-list-sqlvec {:columns {"id" "my_id" "name" "my_name"}})]
          (or (= r ["select id as my_id, name as my_name from test"])
              (= r ["select name as my_name, id as my_id from test"]))))
    (is (= ["select * from test order by id desc"]
           (sql-param-sqlvec {:id-order "desc"})))
    (is (= ["select * from test\nwhere id = ?" 42]
           (select-namespaced-keyword-sqlvec {:test/id 42}))))

  (testing "identifier quoting"
    (is (= ["select * from \"schema\".\"te\"\"st\""]
           (identifier-param-sqlvec
            {:table-name "schema.te\"st"}
            {:quoting :ansi})))
    (is (= ["select * from \"schema\".\"te\"\"st\" as \"my.test\""]
           (identifier-param-sqlvec
            {:table-name ["schema.te\"st" "my.test"]}
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
    (is (= ["select \"test\".\"id\" as \"my.id\", \"test\".\"name\" as \"my.name\" from test"]
           (identifier-param-list-sqlvec
            {:columns [["test.id" "my.id"], ["test.name" "my.name"]]}
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

  (testing "Snippets"
    (is (= ["select id, name"] (select-snip {:cols ["id","name"]})))
    (is (= ["from test"] (from-snip {:tables ["test"]})))
    (is (= ["select id, name\nfrom test\nwhere id = ? or id = ?\norder by id" 1 2]
           (snip-query-sqlvec
            {:select (select-snip {:cols ["id","name"]})
             :from (from-snip {:tables ["test"]})
             :where (where-snip {:cond [(cond-snip {:conj "" :cond ["id" "=" 1]})
                                        (cond-snip {:conj "or" :cond ["id" "=" 2]})]})
             :order (order-snip {:fields ["id"]})}))))

  (testing "metadata"
    (is (:private (meta #'a-private-fn)))
    (is (:private (meta #'another-private-fn)))
    (is (= 1 (:one (meta #'user-meta))))
    (is (= 2 (:two (meta #'user-meta)))))

  (testing "command & result as metadata"
    (is (= :? (:command (meta #'select-one-test-by-id))))
    (is (= :1 (:result (meta #'select-one-test-by-id))))
    (is (= :? (:command (meta #'a-private-fn))))
    (is (= :* (:result (meta #'a-private-fn)))))

  (testing "map of fns"
    (let [db-fns (hugsql/map-of-db-fns "hugsql/sql/test.sql")
          sql-fns (hugsql/map-of-sqlvec-fns "hugsql/sql/test.sql")
          db-fns-str (hugsql/map-of-db-fns-from-string hugsql-string-defs)
          sql-fns-str (hugsql/map-of-sqlvec-fns-from-string hugsql-string-defs)]
      (is (fn? (get-in db-fns [:one-value-param :fn])))
      (is (fn? (get-in db-fns [:select-snip :fn])))
      (is (= "One value param" (get-in db-fns [:one-value-param :meta :doc])))
      (is (not (get-in db-fns [:one-value-param :meta :snip?])))
      (is (get-in db-fns [:select-snip :meta :snip?]))
      (is (fn? (get-in sql-fns [:one-value-param-sqlvec :fn])))
      (is (fn? (get-in sql-fns [:select-snip :fn])))
      (is (= "One value param (sqlvec)" (get-in sql-fns [:one-value-param-sqlvec :meta :doc])))
      (is (fn? (get-in db-fns-str [:test3-select :fn])))
      (is (fn? (get-in sql-fns-str [:test3-select-sqlvec :fn])))))

  (testing "missing header :name, :name-, :snip, or :snip-"
    (is (thrown-with-msg? ExceptionInfo
                          #"Missing HugSQL Header of "
                          (hugsql/def-db-fns-from-string
                            "-- :name: almost-a-yesql-name-hdr\nselect * from test"))))

  (testing "nil header :name, :name-, :snip, or :snip-"
    (is (thrown-with-msg? ExceptionInfo
                          #"HugSQL Header .* not given."
                          (hugsql/def-db-fns-from-string
                            "-- :name \nselect * from test"))))

  (testing "value parameters allow vectors for ISQLParameter/etc overrides"
    (is (= ["insert into test (id, myarr) values (?, ?)" 1 [1 2 3]]
           (hugsql/sqlvec
            "insert into test (id, myarr) values (:id, :v:myarr)"
            {:id 1
             :myarr [1 2 3]})))
    (is (= ["insert into test (id, myarr) values (?,?),(?,?)"
            1 [1 2 3] 2 [4 5 6]]
           (hugsql/sqlvec
            "insert into test (id, myarr) values :t*:records"
            {:records [[1 [1 2 3]]
                       [2 [4 5 6]]]}))))

  (testing "spacing around Raw SQL parameters"
    (is (= ["select col_1 from test"]
           (hugsql/sqlvec
            "select col_:sql:col_num from test"
            {:col_num 1}))))

  (doseq [[db-name db-spec] dbs]
    (doseq [[adapter-name adapter] adapters]
      (let [db (or (adapter-name db-spec) (:default db-spec))]

        (testing "throw if adapter not set"
          (hugsql/set-adapter! nil)
          (is (thrown-with-msg? ExceptionInfo
                                #"No adapter set: use set-adapter!"
                                (one-value-param db {:x 1}))))

        (testing "adapter set"
          (is (satisfies? hugsql.adapter/HugsqlAdapter (hugsql/set-adapter! adapter))))

        (testing "parameter placeholder vs data mismatch"
          (is (thrown-with-msg? ExceptionInfo
                                #"Parameter Mismatch: :id parameter data not found."
                                (one-value-param-sqlvec {:x 1})))
          (is (thrown-with-msg? ExceptionInfo
                                #"Parameter Mismatch: :id parameter data not found."
                                (one-value-param db {:x 1})))
          ;; does not throw on false
          (is (= ["select * from test where id = ?" false]
                 (one-value-param-sqlvec {:id false})))

          ;; deep-get check
          (is (thrown-with-msg? ExceptionInfo
                                #"Parameter Mismatch: :emps.0.id parameter data not found."
                                (hugsql/sqlvec "select * from emp where id = :emps.0.id"
                                               {:emps [{:not-id 1}]})))

          ;; namespaced keywords
          (is (thrown-with-msg? ExceptionInfo
                                #"Parameter Mismatch: :emp/id parameter data not found."
                                (hugsql/sqlvec "select * from emp where id = :emp/id"
                                               {:id 42}))))

        (testing "database commands/queries"
          (condp = db-name
            :mysql (is (= 0 (create-test-table-mysql db)))
            :h2 (is (= 0 (create-test-table-h2 db)))
            :hsqldb (is (= 0 (create-test-table-hsqldb db)))
            (is (= 0 (create-test-table db))))
          (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
          (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))

          (testing "tuple"
            ;; tuple use is not supported by certain dbs
            (when (not-any? #(= % db-name) [:derby :sqlite :hsqldb])
              (is (= [{:id 1 :name "A"}] (tuple-param db {:id-name [1 "A"]})))))

          (testing "insert multiple values"
            ;; only hsqldb appears to not support multi-insert values for :tuple*
            (when (not (= db-name :hsqldb))
              (is (= 3 (insert-multi-into-test-table db {:values [[4 "D"] [5 "E"] [6 "F"]]})))))

          (testing "returning"
            ;; returning support is lacking in many dbs
            (when (not-any? #(= % db-name) [:mysql :h2 :derby :sqlite :hsqldb])
              (is (= [{:id 7}]
                     (insert-into-test-table-returning db {:id 7 :name "G"})))))

          (testing "insert w/ return of .getGeneratedKeys"
            ;; return generated keys, which has varying support and return values
            ;; clojure.java.jdbc returns a hashmap, clojure.jdbc returns a vector of hashmaps
            (when (= adapter-name :clojure.java.jdbc)
              (condp = db-name
                :postgresql
                (is (= {:id 8 :name "H"}
                       (insert-into-test-table-return-keys db {:id 8 :name "H"} {})))

                :mysql
                (is (= {:generated_key 9}
                       (insert-into-test-table-return-keys db {:id 9 :name "I"})))

                :sqlite
                (is (= {(keyword "last_insert_rowid()") 10}
                       (insert-into-test-table-return-keys db {:id 10 :name "J"} {})))

                :h2
                (is (= {(keyword "scope_identity()") 11}
                       (insert-into-test-table-return-keys db {:id 11 :name "J"} {})))

                ;; hsql and derby don't seem to support .getGeneratedKeys
                nil))

            (when (= adapter-name :clojure.jdbc)
              (condp = db-name
                :postgresql
                (is (= [{:id 8 :name "H"}]
                       (insert-into-test-table-return-keys db {:id 8 :name "H"} {})))

                :mysql
                (is (= [{:generated_key 9}]
                       (insert-into-test-table-return-keys db {:id 9 :name "I"})))

                :sqlite
                (is (= [{(keyword "last_insert_rowid()") 10}]
                       (insert-into-test-table-return-keys db {:id 10 :name "J"} {})))

                :h2
                (is (= [{(keyword "scope_identity()") 11}]
                       (insert-into-test-table-return-keys db {:id 11 :name "J"} {})))

                ;; hsql and derby don't seem to support .getGeneratedKeys
                nil)))

          (is (= 1 (update-test-table db {:id 1 :name "C"})))
          (is (= {:id 1 :name "C"} (select-one-test-by-id db {:id 1})))
          (is (= {:id 1 :name "C"} (select-deep-get db {:records [{:id 1}]})))
          (is (= {:id 1 :name "C"} (select-namespaced-keyword db {:test/id 1})))
          (is (= {:id 1 :name "C"} (select-namespaced-keyword-deep-get db {:test.x/records [{:id 1}]})))
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
                                   {:cols ["name"] :sort-by ["name"]} {} {:as-arrays? true}))))

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
          (is (= 0 (drop-test-table db))))

        (testing "snippets"
          (is (= 0 (create-test-table db)))
          (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
          (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))

          (is (= [{:id 1 :name "A"}
                  {:id 2 :name "B"}]
                 (snip-query
                  db
                  {:select (select-snip {:cols ["id","name"]})
                   :from (from-snip {:tables ["test"]})
                   :where (where-snip {:cond [(cond-snip {:conj "" :cond ["id" "=" 1]})
                                              (cond-snip {:conj "or" :cond ["id" "=" 2]})]})
                   :order (order-snip {:fields ["id"]})})))

          (is (= 0 (drop-test-table db))))

        (when (and (= db-name :postgresql) (= adapter-name :clojure.java.jdbc))
          (testing "command & result used in public and private fns"
            (is (= 0 (create-test-table db)))
            (is (= 1 (insert-into-test-table db {:id 1 :name "A"})))
            (is (= 1 (insert-into-test-table db {:id 2 :name "B"})))

            (is (= {:id 1} (update-test-table-returning db {:id 1 :name "C"})))
            (is (= {:id 2} (update-test-table-returning-private db {:id 2 :name "D"})))

            (is (= 0 (drop-test-table db)))))))))
