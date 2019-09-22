(defproject com.layerware/hugsql-adapter-next-jdbc "0.5.0"
  :description "next.jdbc hugsql adapter"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:dir ".."}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [seancorfield/next.jdbc "1.0.7"]
                 [com.layerware/hugsql-adapter "0.5.0"]]
  :profiles {:dev {:dependencies [[com.layerware/hugsql-core "0.5.0"]
                                  [com.h2database/h2 "1.4.199"]]}})
