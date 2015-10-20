(defproject com.layerware/hugsql "0.1.0-SNAPSHOT"
  :description "A Clojure library for embracing SQL"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.layerware/hugsql-core "0.1.0-SNAPSHOT"]
                 [com.layerware/hugsql-clojure-java-jdbc "0.1.0-SNAPSHOT"]]
  :profiles {:dev
             {:plugins [[codox "0.8.13"]
                        [lein-sub "0.3.0"]]
              :dependencies []
              :codox {:src-dir-uri "http://github.com/layerware/hugsql/blob/0.1.0/"
                      :src-linenum-anchor-prefix "L"
                      :output-dir "../gh-pages"
                      :sources ["hugsql-core/src"
                                "hugsql-clojure-java-jdbc/src"
                                "hugsql-clojure-jdbc/src"]}
              :global-vars {*warn-on-reflection* false
                            *assert* false}}})
