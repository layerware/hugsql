(ns hugsql.fragments
  "Fragment-specific code. For the average user, fragments are similar to
   snippets in that they allow one to build SQL queries, except without
   having to pass in the fragment as a param. Under the hood however,
   fragments work very differently from other params; in particular, they
   are expanded before all other params are applied and have their own
   separate, hidden registry."
  (:require [clojure.set :as cset]))

;; Make atoms private so that they are only accessible via functions

(def ^{:dynamic true :private true} frag-ans-atom
  "Atom storing a map between fragment names and sets of ancestors (c.f.
   adjacency list). Used to quickly check for cyclic dependencies."
  (atom {}))

(def ^{:dynamic true :private true} frag-sql-atom
  "Atom storing a map between fragment names and their SQL templates.
   Ancestor fragments in templates should already have been expanded."
  (atom {}))

(defn frag-ancestors
  "Return the ancestors of a fragment beyond that fragment's immediate
   ancestors. Throws an exception upon detecting a cycle."
  [pdef]
  (let [{:keys [hdr sql]} pdef
        frag-name (->> hdr :frag first keyword)
        frag-ans  (->> sql
                       (filter #(and (map? %) (= :frag (:type %))))
                       (map #(-> % :name keyword))
                       set)]
    (if (contains? frag-ans frag-name)
      ;; No self loops
      (throw (ex-info (str "Fragment " frag-name " contains itself.\n"
                           "Immediate ancestors: " (pr-str frag-ans))
                      {}))
      (let [deep-ans (->> frag-ans
                          (map @frag-ans-atom)
                          (apply cset/union))
            all-ans  (cset/union frag-ans deep-ans)]
        ;; No cycles
        (if (contains? all-ans frag-name)
          (throw (ex-info (str "Fragment " frag-name " has cyclic dependency.\n"
                               "All ancestors: " (pr-str all-ans))
                          {}))
          ;; We're good
          all-ans)))))

;; Called during runtime SQL statement prep, i.e. when evaluating Clojure exprs.
(defn expand-fragments*
  "Given a vector of sql strings and param maps, expand out any fragments,
   represented as hashmap parameters with a `:frag` key. Throws an exception
   if an unknown fragment is encountered."
  [sql-template]
  (loop [sql-temp  sql-template
         sql-temp' '[]]
    (if-some [sql-elem (first sql-temp)]
      (cond
        (and (map? sql-elem)
             (= :frag (:type sql-elem)))
        (if-some [frag-sql (@frag-sql-atom (:name sql-elem))]
          (recur (rest sql-temp)
                 (concat sql-temp' frag-sql))
          (throw (ex-info
                  (str "Unknown ancestor fragment: " (:name sql-elem) "\n"
                       "SQL: " (pr-str sql-template))
                  {})))

        :else
        (recur (rest sql-temp)
               (concat sql-temp' [sql-elem]))) ; force conj at end of list
      sql-temp')))

;; Called during initial HugSql function definition.
;; (Analogous to "compile-time" expansion, e.g. Clojure macros.)
(defn expand-fragments
  "Given a parsed def, update the `:sql` value such that all fragments are
   expanded. Throws an exception if an unknown fragment is encountered."
  [pdef]
  (update pdef :sql expand-fragments*))

(defn register-fragment
  "Given a parsed def `exp-pdef` with all ancestor frags already expanded,
   store it in the fragment registry."
  [exp-pdef ans]
  (let [{:keys [hdr sql]} exp-pdef]
    (when-some [frag (:frag hdr)]
      (let [frag-name (->> frag first keyword)]
        (swap! frag-ans-atom assoc frag-name ans)
        (swap! frag-sql-atom assoc frag-name sql)
        nil))))
