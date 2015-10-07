(defproject com.layerware/hugsql "0.1.0-SNAPSHOT"
  :description "A Clojure library for embracing SQL"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.3.7"]]
  :profiles {:dev {:plugins [[lein-auto "0.1.2"]
                             [codox "0.8.13"]]
                   :codox {:src-dir-uri "http://github.com/layerware/hugsql/blob/0.1.0/"
                           :src-linenum-anchor-prefix "L"
                           :output-dir "../gh-pages"}
                   :global-vars {*warn-on-reflection* false
                                 *assert* false}}})
