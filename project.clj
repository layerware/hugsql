(defproject com.layerware/hugsql "0.1.0"
  :description "A Clojure library for embracing SQL"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [com.layerware/hugsql-core "0.1.0"]
                 [com.layerware/hugsql-adapter-clojure-java-jdbc "0.1.0"]]
  :profiles {:dev
             {:plugins [[lein-codox "0.9.0"]
                        [lein-sub "0.3.0"]]
              :sub ["hugsql-core"
                    "hugsql-adapter-clojure-java-jdbc"
                    "hugsql-adapter-clojure-jdbc"]
              :dependencies [[com.layerware/hugsql-adapter-clojure-jdbc "0.1.0"]]
              :codox {:source-uri "http://github.com/layerware/hugsql/blob/0.1.0/{filepath}#L{line}"
                      :output-path "../gh-pages"
                      :source-paths ["hugsql-core/src"
                                     "hugsql-adapter-clojure-java-jdbc/src"
                                     "hugsql-adapter-clojure-jdbc/src"]}
              :global-vars {*warn-on-reflection* false
                            *assert* false}}})
