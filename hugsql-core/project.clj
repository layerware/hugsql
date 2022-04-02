(defproject com.layerware/hugsql-core "0.5.2"
  :description "HugSQL core functionality without adapter dependencies"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:dir ".."}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.reader "1.3.6"]
                 [com.layerware/hugsql-adapter "0.5.2"]]
  :profiles {:dev
             {:plugins [[lein-auto "0.1.2"]]
              :dependencies [[com.layerware/hugsql-adapter-clojure-java-jdbc "0.5.2"]
                             [com.layerware/hugsql-adapter-clojure-jdbc "0.5.2"]
                             [com.layerware/hugsql-adapter-next-jdbc "0.5.2"]
                             [org.postgresql/postgresql "42.2.8"]
                             [mysql/mysql-connector-java "8.0.17"]
                             [org.xerial/sqlite-jdbc "3.28.0"]
                             [org.apache.derby/derby "10.14.2.0"]
                             [hsqldb/hsqldb "1.8.0.10"]
                             [com.h2database/h2 "1.4.196"]]
              :global-vars {*warn-on-reflection* false
                            *assert* false}}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.3"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.0"]]}}
  :aliases {"test-all" ["with-profile" "dev,1.10:dev,1.11" "test"]})
