(ns hugsql.core-test
  (:require [clojure.test :refer :all]
            ;[clojure.java.classpath :as cp]
            [hugsql.core :as hugsql]))


(def dbs
  {:postgresql  {:subprotocol "postgresql"
                 :subname "//127.0.0.1:5432/pgtest"
                 :user "pgtest"
                 :password "pgtest"}})

(hugsql/def-sql-string-fns "hugsql/sql/test.sql")

;; (testing "fn creation"
;;   (is (= nil (no-params-select-sql db-spec {}))))

(prn (ns-publics *ns*))


