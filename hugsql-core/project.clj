(defproject com.layerware/hugsql-core "0.1.0"
  :description "hugsql core functionality without adapter dependencies"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]]
  :profiles {:dev
             {:plugins [[lein-auto "0.1.2"]]
              :dependencies [[com.layerware/hugsql-adapter-clojure-java-jdbc "0.1.0"]
                             [com.layerware/hugsql-adapter-clojure-jdbc "0.1.0"]
                             [org.postgresql/postgresql "9.4-1201-jdbc41"]
                             [mysql/mysql-connector-java "5.1.37"]
                             [org.xerial/sqlite-jdbc "3.8.11.2"]
                             [org.apache.derby/derby "10.12.1.1"]
                             [hsqldb/hsqldb "1.8.0.10"]
                             [com.h2database/h2 "1.4.190"]]
              :global-vars {*warn-on-reflection* false
                            *assert* false}}})
