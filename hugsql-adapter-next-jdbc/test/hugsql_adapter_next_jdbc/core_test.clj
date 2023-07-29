(ns hugsql-adapter-next-jdbc.core-test
  (:require [clojure.test :refer :all]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-jdbc]
            [next.jdbc.result-set :as result-set]
            [hugsql-adapter-next-jdbc.core-test.sql :as sql]))

(def db {:dbtype "h2:mem" :dbname "test"})

(deftest adapter-tests
  (hugsql/set-adapter! (next-jdbc/hugsql-adapter-next-jdbc))

  (testing "create table"
    (is (not (nil? (sql/create-colors-table db)))))
  (testing "insert a row"
    (is (= (sql/insert-color db {:name "ocher" :r 204 :g 119 :b 34})
           [{:next.jdbc/update-count 1}])))
  (testing "insert another row"
    (is (= (sql/insert-color db {:name "crimson" :r 220 :g 20 :b 60})
           [{:next.jdbc/update-count 1}])))
  (testing ":insert command test"
    (is (= (sql/insert-color-alt db {:name "crimson" :r 220 :g 20 :b 60})
           [{:id 3}])))
  (testing "insert and return affected rows"
    (is (= (sql/update-color-affected db {:r 100})
           3)))
  (testing "select a row"
    (is (= (:color/id (sql/select-color-by-id db {:id 1}))
           1)))
  (testing "select multiple rows"
    (is (= (count (sql/select-all-colors db))
           3)))

  (testing "select a row w/ custom builder fn"
    (let [adapter (next-jdbc/hugsql-adapter-next-jdbc {:builder-fn result-set/as-unqualified-maps})]
      (is (= (:color/id (sql/select-color-by-id db {:id 1} {:adapter adapter}))
             1))))

  (testing "drop table"
    (is (not (nil? (sql/drop-colors-table db))))))
