(ns hugsql.unbound-fn
  (:require  [clojure.test :as t]
             [hugsql.core :as hugsql]))

(hugsql/def-sqlvec-fns "hugsql/sql/test.sql")

(t/deftest unbound-fn
  (let [es (java.util.concurrent.Executors/newFixedThreadPool 20)]
    (try
      (dotimes [i 1000]
        (doseq [[s v] (ns-publics 'hugsql.expr-run)]
          (ns-unmap 'hugsql.expr-run s))
        (->> (.invokeAll es (for [j (range 10)]
                              (fn [] (clj-expr-generic-update-sqlvec {:table "test"
                                                                      :updates {:name "X"}
                                                                      :id i}))))
             (mapv deref)))
      (catch Exception e
        (throw e))
      (finally (.shutdown es)))))
