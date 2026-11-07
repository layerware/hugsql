(ns hugsql.macros-test
  "hugsql.macros on Clojure and ClojureScript: the SQL file is the one
   core-test uses, and the sqlvecs match core-test's, so both platforms
   generate the same SQL from the same file."
  (:require #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])
            [hugsql.adapter :as adapter]
            [hugsql.core :as hugsql]
            [hugsql.macros :as macros]))

(macros/def-sqlvec-fns "hugsql/sql/test.sql")

(def adapter-stub
  "Returns the call it got: `{:command [sqlvec ...] :result ...}`."
  (reify adapter/HugsqlAdapter
    (execute [_ db sqlvec options] {:command [:execute db sqlvec (:command-options options)]})
    (query [_ db sqlvec options] {:command [:query db sqlvec (:command-options options)]})
    (result-one [_ result _] (assoc result :result :one))
    (result-many [_ result _] (assoc result :result :many))
    (result-affected [_ result _] (assoc result :result :affected))
    (result-raw [_ result _] (assoc result :result :raw))
    (on-exception [_ e] (throw e))))

(def file-fns (macros/map-of-db-fns "hugsql/sql/test.sql" {:adapter adapter-stub}))

(defn- db-fn [k] (get-in file-fns [k :fn]))

(macros/def-db-fns-from-string
  "-- :name from-string-select :? :*\nselect * from test where id = :id
-- :name from-string-insert :! :n\ninsert into test (id) values (:id)"
  {:adapter adapter-stub})

(deftest sqlvec-fns
  (testing "every parameter type"
    (is (= ["select * from test"] (no-params-select-sqlvec)))
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
    (is (= ["select * from test as my_test"]
           (identifier-param-sqlvec {:table-name ["test" "my_test"]})))
    (is (= ["select id as my_id, name as my_name from test"]
           (identifier-param-list-sqlvec {:columns [["id" "my_id"], ["name" "my_name"]]})))
    (is (= ["select * from test order by id desc"]
           (sql-param-sqlvec {:id-order "desc"})))
    (is (= ["select * from test\nwhere id = ?" 42]
           (select-namespaced-keyword-sqlvec {:test/id 42}))))

  (testing "identifier quoting"
    (is (= ["select * from \"schema\".\"te\"\"st\""]
           (identifier-param-sqlvec {:table-name "schema.te\"st"} {:quoting :ansi}))))

  (testing "Clojure expressions, single- and multi-line"
    (is (= ["select\nname\nfrom test\norder by id"]
           (clj-expr-single-sqlvec {:cols ["name"]})))
    (is (= ["select\n*\nfrom test\norder by id"]
           (clj-expr-single-sqlvec {})))
    (is (= ["select\n\nname\n\nfrom test\norder by id"]
           (clj-expr-multi-sqlvec {:cols ["name"]})))
    (is (= ["select\n\n*\n\nfrom test\norder by id"]
           (clj-expr-multi-sqlvec {}))))

  (testing "an expression's :require header aliases and refers resolve"
    (is (= ["update test set\nname = ?\nwhere id = ?" "X" 3]
           (clj-expr-generic-update-sqlvec {:table "test" :updates {:name "X"} :id 3}))))

  (testing "snippets"
    (is (= ["select id, name\nfrom test\nwhere id = ? or id = ?\norder by id" 1 2]
           (snip-query-sqlvec
            {:select (select-snip {:cols ["id","name"]})
             :from (from-snip {:tables ["test"]})
             :where (where-snip {:cond [(cond-snip {:conj "" :cond ["id" "=" 1]})
                                        (cond-snip {:conj "or" :cond ["id" "=" 2]})]})
             :order (order-snip {:fields ["id"]})}))))

  (testing "doc metadata"
    (is (= "One value param (sqlvec)" (:doc (meta #'one-value-param-sqlvec))))))

(deftest db-fns
  (testing "the header's command and result pick the adapter fns"
    (is (= {:command [:query :db ["select * from test where id = ?" 1] nil]
            :result :one}
           ((db-fn :select-one-test-by-id) :db {:id 1})))
    (is (= {:command [:execute :db ["insert into test (id, name) values (?, ?)" 1 "A"] nil]
            :result :affected}
           ((db-fn :insert-into-test-table) :db {:id 1 :name "A"})))
    (is (= :? (get-in file-fns [:select-one-test-by-id :meta :command])))
    (is (get-in file-fns [:select-snip :meta :snip?])))

  (testing "command options pass through"
    (is (= [:query :db ["select * from test where id = ?" 1] [:x]]
           (:command ((db-fn :select-one-test-by-id) :db {:id 1} {} :x)))))

  (testing "from a string"
    (is (= {:command [:query :db ["select * from test where id = ?" 2] nil]
            :result :many}
           (from-string-select :db {:id 2})))
    (is (= {:command [:execute :db ["insert into test (id) values (?)" 3] nil]
            :result :affected}
           (from-string-insert :db {:id 3})))))

(deftest maps-of-fns
  (let [fns (macros/map-of-sqlvec-fns "hugsql/sql/test.sql")]
    (is (= ["select * from test where id = ?" 1]
           ((get-in fns [:one-value-param-sqlvec :fn]) {:id 1})))
    (is (= "One value param (sqlvec)" (get-in fns [:one-value-param-sqlvec :meta :doc])))))

(deftest runtime-fns
  (is (= ["select * from test where id = ?" 1]
         (hugsql/sqlvec "select * from test where id = :id" {:id 1}))))
