(ns hugsql-adapter-next-jdbc.core-test
  (:require [clojure.test :refer :all]
            [next.jdbc.result-set :as result-set]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-jdbc]))

(hugsql/def-db-fns "./hugsql_adapter_next_jdbc/queries.sql")
(hugsql/set-adapter! (next-jdbc/hugsql-adapter-next-jdbc))

(def conn {:dbtype "h2:mem" :dbname "test"})

(deftest create-table-test
  (testing "Can create a table."
    (is (not (nil? (create-colors-table conn))))))

(deftest row-tests
  (testing "Can insert a row."
    (is (= (insert-color conn {:name "ocher" :r 204 :g 119 :b 34})
           [{:next.jdbc/update-count 1}])))
  (testing "Can insert another row."
    (is (= (insert-color conn {:name "crimson" :r 220 :g 20 :b 60})
           [{:next.jdbc/update-count 1}])))
  (testing ":insert command test"
    (is (= (insert-color-alt conn {:name "crimson" :r 220 :g 20 :b 60})
           [{:COLORS/ID 3}])))
  (testing "Can insert and return affected rows"
    (is (= (update-color-affected conn {:r 100})
           3)))
  (testing "Can select a row."
    (is (= (:COLORS/ID (select-color-by-id conn {:id 1}))
           1)))
  (testing "Can select multiple rows."
    (is (= (count (select-all-colors conn))
           3))))

(deftest custom-builder-fn-test
  (hugsql/set-adapter! (next-jdbc/hugsql-adapter-next-jdbc {:builder-fn result-set/as-unqualified-maps}))
  (testing "Can select a row w/ custom builder fn"
    (is (= (:ID (select-color-by-id conn {:id 1}))
           1))))