(ns princess-bride.db
  (:require [hugsql.core :as hugsql]))

(def db
  {:subprotocol "h2"
   :subname (str (System/getProperty "java.io.tmpdir")
              "/princess_bride.h2")})
