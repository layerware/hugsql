(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.edn :as edn]
            [borkdude.rewrite-edn :as rewrite-edn]))

(def group "com.layerware")

;; these match lib paths, deps.edn aliases; order matters!
(def libs ["hugsql-adapter"
           "hugsql-adapter-clojure-java-jdbc"
           "hugsql-adapter-clojure-jdbc"
           "hugsql-adapter-next-jdbc"
           "hugsql-core"
           "hugsql"])

(defn- lib-symbol [lib]
  (symbol group lib))

(defn- deps-edn []
  (->> (slurp "deps.edn")
       (edn/read-string)))

(defn- lib-src [lib]
  (-> (deps-edn)
      (get-in [:aliases (keyword (format "%s-src" lib))])))

(defn- set-version-in-lib [lib version]
  ;; update versions for all hugsql libs in each of the lib deps.edn files
  (let [deps-edn (format "%s/deps.edn" lib)
        nodes    (->> deps-edn
                      (slurp)
                      (rewrite-edn/parse-string))]
    (->> libs
         (reduce (fn [nodes l]
                   (if (rewrite-edn/sexpr (rewrite-edn/get-in nodes [:deps (lib-symbol l)]))
                     (rewrite-edn/assoc-in nodes [:deps (lib-symbol l)] {:mvn/version version})
                     nodes))
                 nodes)
         (spit deps-edn))))

(defn- set-version-in-libs [version]
  (doseq [lib libs]
    (set-version-in-lib lib version)))

(defn- set-version-in-docs [version]
  (spit "docs/hugsql.org/src/version.js"
        (format "export const hugsqlVersion = '%s';\n" version)))

(defn- get-version []
  (-> (slurp "version.edn")
      (edn/read-string)
      (:version)))

(defn- set-version [{:keys [version]}]
  (let [full-version (format "%s.%s" version (b/git-count-revs nil))]
    (->> full-version
         (hash-map :version)
         (pr-str)
         (spit "version.edn"))
    (set-version-in-libs full-version)
    (set-version-in-docs full-version)))

(defn version
  "Get or set the version."
  ([] (version {}))
  ([{:keys [version]}]
   (when version
     (set-version {:version version}))
   (println (get-version))))


(defn- class-dir [lib]
  (format "%s/target/classes" lib))

(defn- basis [lib]
  (b/create-basis {:project (format "%s/deps.edn" lib)}))

(defn- src-pom [lib]
  (format "%s/pom-template.xml" lib))

(defn- jar-file [lib]
  (format "%s/target/%s-%s.jar" lib lib (get-version)))

(defn- clean-lib [lib]
  (b/delete {:path (format "%s/target" lib)}))

(defn clean [_]
  (doseq [lib libs]
    (clean-lib lib)))

(defn- jar [lib]
  (b/write-pom {:src-pom   (src-pom lib)
                :class-dir (class-dir lib)
                :lib       (lib-symbol lib)
                :version   (get-version)
                :basis     (basis lib)
                :src-dirs  (lib-src lib)})
  (b/copy-dir {:src-dirs   (lib-src lib)
               :target-dir (class-dir lib)})
  (b/jar {:class-dir (class-dir lib)
          :jar-file (jar-file lib)}))

(defn- install-lib [lib]
  (b/install {:class-dir (class-dir lib)
              :lib       (lib-symbol lib)
              :version   (get-version)
              :basis     (basis lib)
              :src-dirs  (lib-src lib)
              :jar-file  (jar-file lib)}))


(defn jars
  "Build and install jars locally.
   Due to dependencies, each lib jars must be installed
   locally before building subsequent lib jars."
  [_]
  (doseq [lib libs]
    (jar lib)
    (install-lib lib)))

(comment

  (get-version)
  (set-version {:version "1.0"})
  (version)
  (version {:version "1.0"})

  (clean {})
  (jars {})

  ;;
  )