(ns hugsql.fragments
  "Fragment-specific code."
  (:require [clojure.set :as cset]))

;; Make atoms private so that they are only accessible via functions

(def ^{:dynamic :private} frag-ans-atom
  "Atom storing a map between fragment names and sets of ancestors.
   Needed to check for cyclic dependencies."
  (atom {}))

(def ^{:dynamic :private} frag-sql-atom
  "Atom storing a map between fragment names and their SQL templates.
   Ancestor fragments in templates should already have been expanded."
  (atom {}))

(defn- all-frag-ancestors
  "Return the ancestors of a fragment beyond that fragment's immediate
   ancestors. Throws an exception upon detecting a cycle."
  [frag-name frag-ans]
  (if (contains? frag-ans frag-name)
    ;; No self loops
    (throw (ex-info (str "Fragment " frag-name " contains itself!\n"
                         "Immediate ancestors: " (pr-str frag-ans))
                    {}))
    (let [deep-ans (->> frag-ans
                        (map @frag-ans-atom)
                        (apply cset/union))
          all-ans  (cset/union frag-ans deep-ans)]
      ;; No cycles
      (if (contains? all-ans frag-name)
        (throw (ex-info (str "Fragment " frag-name " has cyclic dependency!\n"
                             "All ancestors: " (pr-str all-ans))
                        {}))
        ;; We're good
        all-ans))))

(defn expand-fragments*
  "Given `sql-template` produced by `parse`, expand out any fragments,
   represented as hashmap parameters with a `:frag` key. Throws an exception
   if an unknown fragment is encountered."
  [sql-template]
  (loop [sql-temp  sql-template
         sql-temp' '[]]
    (if-some [{param-type :type param-name :name :as sql-elem}
              (first sql-temp)]
      (if (= :frag param-type)
        (if-some [frag-sql (@frag-sql-atom param-name)]
          (recur (rest sql-temp)
                 (concat sql-temp' frag-sql))
          (throw (ex-info (str "Unknown ancestor fragment " param-name "!\n"
                               "SQL: " (pr-str (sql-template)))
                          {})))
        (recur (rest sql-temp)
               (concat sql-temp' [sql-elem]))) ; force conj at end of list
      (do
        ;; (println (format "sql-template: %s" (pr-str (type sql-temp))))
        ;; (println (format "sql-template': %s" (pr-str (type sql-temp'))))
        sql-temp'))))

(defn expand-fragments
  [pdef]
  (update pdef :sql expand-fragments*)
  #_(let [pdef' (update pdef :sql expand-fragments*)]
    (when (not= pdef pdef') (println (pr-str pdef')))
    pdef'))

(defn register-fragment!
  "Given a parsed def `pdef`, validate and store in the fragment registry.
   Expands any ancestor fragments before storing. Throws an exception if
   a cyclic dependency is encountered"
  [pdef]
  (let [{:keys [hdr sql]} pdef]
    (when-some [frag (:frag hdr)]
      (let [frag-name (->> frag first keyword)
            frag-ans  (->> sql
                           (filter #(and (map? %) (= :frag (:type %))))
                           (map #(-> % :name keyword))
                           set)
            all-ans   (all-frag-ancestors frag-name frag-ans)]
        (swap! frag-ans-atom assoc frag-name all-ans)
        (swap! frag-sql-atom assoc frag-name sql)
        nil))))

(comment
  (reset! frag-ans-atom {})
  (reset! frag-sql-atom {})

  (reset! frag-ans-atom {:foo #{}})
  (reset! frag-sql-atom {:foo [{:name ["bar"]}]})
  (expand-fragments
   [{:name ["bee"]} {:frag ["foo"]}]))
