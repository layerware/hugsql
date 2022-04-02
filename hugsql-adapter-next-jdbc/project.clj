(defproject com.layerware/hugsql-adapter-next-jdbc "0.5.3"
  :description "next.jdbc hugsql adapter"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :scm {:dir ".."}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.github.seancorfield/next.jdbc "1.2.772"]
                 [com.layerware/hugsql-adapter "0.5.3"]]
  :profiles {:dev {:dependencies [[com.layerware/hugsql-core "0.5.3"]
                                  [com.h2database/h2 "1.4.199"]]}})
