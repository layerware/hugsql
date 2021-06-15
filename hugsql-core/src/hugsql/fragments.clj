(ns hugsql.fragments
  "Fragment-specific code."
  (:require [clojure.set :as cset]
            [clojure.string :as string]))

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

(def ^:private frag-regex #":frag:([\pL\pM\pS\d\_\-\.\+\*\?\/\:]*)")

(defn expand-fragments*
  "Given `sql-template` produced by `parse`, expand out any fragments,
   represented as hashmap parameters with a `:frag` key. Throws an exception
   if an unknown fragment is encountered."
  [sql-template]
  (loop [sql-temp  sql-template
         sql-temp' '[]]
    (if-some [sql-elem (first sql-temp)]
      (cond
        ;; Fragment param - 
        (and (map? sql-elem) (= :frag (:type sql-elem)))
        (if-some [frag-sql (@frag-sql-atom (:name sql-elem))]
          (recur (rest sql-temp)
                 (concat sql-temp' frag-sql))
          (throw (ex-info
                  (str "Unknown ancestor fragment " (:name sql-elem) "!\n"
                       "SQL: " (pr-str (sql-template)))
                  {})))

        ;; Clojure string with fragment
        ;; (and (seq? sql-elem)
        ;;      (string? (first sql-elem))
        ;;      (re-matches frag-regex (first sql-elem)))
        ;; (if-some [frag-sql (->> (first sql-elem)
        ;;                         (re-matches frag-regex)
        ;;                         second
        ;;                         keyword
        ;;                         (get @frag-sql-atom))]
        ;;   (recur (rest sql-temp)
        ;;          (concat sql-temp' ; force conj at end of list
        ;;                  [(string/replace (first sql-elem)
        ;;                                   frag-regex
        ;;                                   (pr-str frag-sql))]))
        ;;   (throw (ex-info
        ;;           (str "Unknown ancestor fragment!\n"
        ;;                "Clojure string:" (first sql-elem)
        ;;                "Full SQL: " (pr-str (sql-template)))
        ;;           {})))

        ;; No fragments found
        :else
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

(defn get-fragment
  [get-vec]
  (let [frag-sql (-> get-vec first (@frag-sql-atom))]
    ;; (println (first get-vec))
    ;; (println frag-sql)
    ;; (println (expand-fragments* frag-sql))
    (vec (expand-fragments* frag-sql))))
