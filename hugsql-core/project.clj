(defproject com.layerware/hugsql-core "0.4.8"
  :description "HugSQL core functionality without adapter dependencies"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:dir ".."}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.reader "1.1.0"]
                 [com.layerware/hugsql-adapter "0.4.8"]]
  :profiles {:dev
             {:plugins [[lein-auto "0.1.2"]]
              :dependencies [[com.layerware/hugsql-adapter-clojure-java-jdbc "0.4.8"]
                             [com.layerware/hugsql-adapter-clojure-jdbc "0.4.8"]
                             [org.postgresql/postgresql "42.1.4"]
                             [mysql/mysql-connector-java "5.1.44"]
                             [org.xerial/sqlite-jdbc "3.20.1"]
                             [org.apache.derby/derby "10.14.1.0"]
                             [hsqldb/hsqldb "1.8.0.10"]
                             [com.h2database/h2 "1.4.196"]]
              :global-vars {*warn-on-reflection* false
                            *assert* false}}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0-beta2"]]}}
  :aliases {"test-all" ["with-profile" "dev,1.6:dev,1.7:dev,1.8:dev,1.9" "test"]})
