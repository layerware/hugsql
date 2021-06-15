(ns hugsql.parser-test
  (:require [clojure.test :refer :all]
            [hugsql.parser :refer [parse]])
  (:import [clojure.lang ExceptionInfo]))

(deftest parsing

  (testing "empty string"
    (is (thrown? ExceptionInfo (parse ""))))

  (testing "windows newlines"
    (is (= 2 (count
              (parse "-- :snip one\r\nselect *\r\n-- :snip two\r\nfrom test")))))

  (testing "SQL comments"
    (is (= [] (parse "-- an sql comment")))
    (is (= [] (parse "/* a\nmulti-line\ncomment */"))))

  (testing "leading SQL comment followed by hugsql comment"
    (is (= [{:hdr  {:name ["query"]
                    :file nil
                    :line 6}
             :sql  ["select * from emp"]}]
           (parse "-- sql comment\n\n\n\n\n-- :name query\nselect * from emp"))))

  (testing "SQL comment in middle of SQL"
    (is (= [{:hdr {:name ["query"]
                   :file nil
                   :line 1}
             :sql ["select * \nfrom emp"]}]
           (parse "-- :name query\nselect * \n-- a comment\nfrom emp")))
    (is (= [{:hdr {:name ["query"]
                   :file nil
                   :line 1}
             :sql ["select * \nfrom emp"]}]
           (parse "-- :name query\nselect * \n--\nfrom emp")))
    (is (= [{:hdr {:name ["query"]
                   :file nil
                   :line 1}
             :sql ["select * \nfrom emp"]}]
           (parse "-- :name query\nselect * \n/* a comment \n*/from emp")))
    (is (= [{:hdr {:name ["query"]
                   :file nil
                   :line 1}
             :sql ["select * \nfrom emp"]}]
           (parse "-- :name query\nselect * \n/* \n*/from emp"))))

  (testing "hugsql header comments"
    (is (= [{:hdr {:name ["test"]
                   :file nil
                   :line 1}
             :sql []}]
           (parse "-- :name test")))
    (is (= [{:hdr {:doc ["this\nis\n\ndoc"]
                   :file nil
                   :line 4}
             :sql []}]
           (parse "/* :doc this\nis\n\ndoc\n*/"))))

  (testing "SQL"
    (testing "no hugsql header"
      (is (thrown? ExceptionInfo (parse "select * from emp")))
      (is (= [{:hdr {}
               :sql ["select * from emp"]}]
             (parse "select * from emp" {:no-header true})))
      (is (= [{:hdr {}
               :sql ["*"]}]
             (parse "*" {:no-header true}))))

    (testing "hugsql header"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp"]}]
             (parse "-- :name test\nselect * from emp")))
      (is (= [{:hdr {:snip ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp"]}]
             (parse "-- :snip test\nselect * from emp")))
      (is (= [{:hdr {:frag ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp"]}]
             (parse "-- :frag test\nselect * from emp"))))

    (testing ":param-name (default value parameter type)"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :v :name :id}]}]
             (parse "-- :name test\nselect * from emp where id = :id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :v :name :employee/id}]}]
             (parse "-- :name test\nselect * from emp where id = :employee/id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :v :name :my.employee/id}]}]
             (parse "-- :name test\nselect * from emp where id = :my.employee/id"))))

    (testing ":param-type:param-name (explicit parameter type)"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :value :name :id}]}]
             (parse "-- :name test\nselect * from emp where id = :value:id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :value :name :employee/id}]}]
             (parse "-- :name test\nselect * from emp where id = :value:employee/id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :value :name :my.employee/id}]}]
             (parse "-- :name test\nselect * from emp where id = :value:my.employee/id"))))

    (testing "Deep get (get-in) parameter name"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :value :name :employees.123.id}]}]
             (parse "-- :name test\nselect * from emp where id = :value:employees.123.id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = "
                     {:type :value :name :my.company/employees.123.id}]}]
             (parse "-- :name test\nselect * from emp where id = :value:my.company/employees.123.id"))))

    (testing "::sometype is a Postgresql cast and not a hugsql param"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select id::text as id-str from emp where id = "
                     {:type :v :name :id}]}]
             (parse "-- :name test\nselect id::text as id-str from emp where id = :id")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = " {:type :v :name :id} "::bigint"]}]
             (parse "-- :name test\nselect * from emp where id = :id::bigint")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where id = " {:type :x :name :id} "::bigint"]}]
             (parse "-- :name test\nselect * from emp where id = :x:id::bigint"))))

    (testing "Escaping colons"
      ;; note that this test must take into account Clojure's string escaping,
      ;; so the test below has a double backslash to represent a single backslash in hugsql
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select my_arr[1:3] from emp where contrived \\ backslash"]}]
             (parse "-- :name test\nselect my_arr[1\\:3] from emp where contrived \\ backslash"))))

    (testing "SQL quoted strings (ignored and do not replace params)"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select \"col1\" from emp"]}]
             (parse "-- :name test\nselect \"col1\" from emp")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from emp where \"col1\" = 'my :param is safe'"]}]
             (parse "-- :name test\nselect * from emp where \"col1\" = 'my :param is safe'")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select 'it''s cool' from emp"]}]
             (parse "-- :name test\nselect 'it''s cool' from emp"))))

    (testing "SQL optimizer hints"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["/*+ foo */ select * from emp"]}]
             (parse "-- :name test\n/*+ foo */ select * from emp"))))

    (testing "Clojure expressions"
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select\n" ["(if (= 1 1) \"Y\" \"N\")" :end] "\nfrom test"]}]
             (parse "-- :name test\nselect\n--~ (if (= 1 1) \"Y\" \"N\")\nfrom test")))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select\n"
                     ["(if (= 1 1)" :cont] "\n'Y'\n" [:cont] "\n'N'\n" [")" :end]
                     "\nfrom test"]}]
             (parse (str "-- :name test\nselect\n/*~ (if (= 1 1) */\n"
                         "'Y'\n/*~*/\n'N'\n/*~ ) ~*/\nfrom test"))))
      (is (= [{:hdr {:name ["test"]
                     :file nil
                     :line 1}
               :sql ["select * from test where\n"
                     ["(if id" :cont] "\nid = " {:type :v :name :id} "\n" [:cont] "\n1=1\n" [")" :end]]}]
             (parse (str "-- :name test\nselect * from test where\n"
                         "/*~ (if id */\nid = :id\n/*~*/\n1=1\n/*~ ) ~*/")))))))
