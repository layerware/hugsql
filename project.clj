(defproject com.layerware/hugsql "0.5.0-SNAPSHOT"
  :description "A Clojure library for embracing SQL"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:name "HugSQL"
        :url "https://github.com/layerware/hugsql"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.layerware/hugsql-core "0.5.0-SNAPSHOT"]
                 [com.layerware/hugsql-adapter-clojure-java-jdbc "0.5.0-SNAPSHOT"]]
  :aliases {"install-all" ["do" ["sub" "install"] "install"]}
  :profiles {:dev
             {:plugins [[lein-codox "0.10.3"]
                        [lein-sub "0.3.0"]]
              :sub ["hugsql-adapter"
                    "hugsql-core"
                    "hugsql-adapter-clojure-java-jdbc"
                    "hugsql-adapter-clojure-jdbc"]
              :dependencies [[com.layerware/hugsql-adapter-clojure-jdbc "0.5.0-SNAPSHOT"]]
              :codox {:source-uri "http://github.com/layerware/hugsql/blob/0.5.0-SNAPSHOT/{filepath}#L{line}"
                      :output-path "../gh-pages"
                      :source-paths ["hugsql-core/src"
                                     "hugsql-adapter/src"
                                     "hugsql-adapter-clojure-java-jdbc/src"
                                     "hugsql-adapter-clojure-jdbc/src"]}
              :global-vars {*warn-on-reflection* false
                            *assert* false}}})
