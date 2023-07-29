(defproject princess-bride "0.5.3"
  :description "The Princess Bride HugSQL Example App"
  :url "https://github.com/layerware/hugsql"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :main princess-bride.core
  :aliases {"as-you-wish" ["run"]}
  :profiles {:uberjar {:aot :all
                       :omit-source true}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.layerware/hugsql "1.0.227"]
                 [com.h2database/h2 "1.4.196"]])
