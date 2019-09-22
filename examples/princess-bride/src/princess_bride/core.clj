(ns princess-bride.core
  (:require [princess-bride.db :refer [db]]
            [princess-bride.db.characters :as characters]
            [princess-bride.db.quotes :as quotes]
            [clojure.pprint :as pprint]
            [clojure.string :as string]
            [clojure.java.jdbc]))

;; save some typing
(def pp pprint/pprint)
(defn ppl [x]
  "pretty print w/ extra trailing line"
  (pp x) (println))

(defn ppr
  "pretty print w/ trailing result indicator ;;=>"
  [x]
  (print (string/replace (with-out-str (pp x)) #"\n$" ""))
  (println "  ;;=>"))

(defn ppsv
  "Pretty print an sqlvec"
  [sv]
  (println
   (string/join ""
                ["[\""
                 (-> (first sv)
                     (string/replace #"\"" "\\\\\"")
                     (string/replace #"\n" "\n  "))
                 "\""
                 (when (seq (rest sv)) "\n,")
                 (string/replace
                  (string/join ","
                               (map #(with-out-str (pp %)) (rest sv)))
                  #"\n$"
                  "")
                 "]\n"])))

(defmacro ex
  "Example macro: Pretty print code, 
   eval, then pretty print result"
  [& code]
  `(do
     (ppr (quote ~@code))
     (ppl ~@code)))

(defmacro exsv
  "Example macro for sqlvec: Pretty print code, 
   eval, then pretty print sqlvec"
  [& code]
  `(do
     (ppr (quote ~@code))
     (ppsv ~@code)))

(defn create-tables []

  (exsv (characters/create-characters-table-sqlvec))
  (ex (characters/create-characters-table db))
  (exsv (quotes/create-quotes-table-sqlvec))
  (ex (quotes/create-quotes-table db)))

(defn drop-tables []

  (exsv (characters/drop-characters-table-sqlvec))
  (ex (characters/drop-characters-table db))
  (exsv (quotes/drop-quotes-table-sqlvec))
  (ex (quotes/drop-quotes-table db)))

(defn inserts []

  ;; single record
  (exsv (characters/insert-character-sqlvec {:name "Westley" :specialty "love"}))
  (ex (characters/insert-character db {:name "Westley" :specialty "love"}))

  (ex (characters/insert-character db {:name "Buttercup" :specialty "beauty"}))


  ;; multiple records


  (exsv (characters/insert-characters-sqlvec {:characters [["Vizzini" "intelligence"]
                                                           ["Fezzik" "strength"]
                                                           ["Inigo Montoya" "swordmanship"]]}))

  (ex (characters/insert-characters db {:characters [["Vizzini" "intelligence"]
                                                     ["Fezzik" "strength"]
                                                     ["Inigo Montoya" "swordmanship"]]}))

  ;; transactions
  (ex (clojure.java.jdbc/with-db-transaction [tx db]
        (characters/insert-character tx {:name "Miracle Max" :specialty "miracles"})
        (characters/insert-character tx {:name "Valerie" :specialty "speech interpreter"}))))

(defn updates []

  (exsv
   (let [vizzini (characters/character-by-name db {:name "vizzini"})]
     (characters/update-character-specialty-sqlvec {:id (:id vizzini)
                                                    :specialty "boasting"})))
  (ex
   (let [vizzini (characters/character-by-name db {:name "vizzini"})]
     (characters/update-character-specialty db {:id (:id vizzini)
                                                :specialty "boasting"}))))

(defn deletes []

  ;; spoiler alert; Vizzini dies, so let's delete him
  (exsv
   (let [vizzini (characters/character-by-name db {:name "vizzini"})]
     (characters/delete-character-by-id-sqlvec {:id (:id vizzini)})))
  (ex
   (let [vizzini (characters/character-by-name db {:name "vizzini"})]
     (characters/delete-character-by-id db {:id (:id vizzini)}))))

(defn selects []

  (exsv (characters/all-characters-sqlvec))
  (ex (characters/all-characters db))

  (exsv (characters/character-by-id-sqlvec {:id 1}))
  (ex (characters/character-by-id db {:id 1}))

  (exsv (characters/character-by-name-sqlvec {:name "buttercup"}))
  (ex (characters/character-by-name db {:name "buttercup"}))

  (exsv (characters/characters-by-name-like-sqlvec {:name-like "%zz%"}))
  (ex (characters/characters-by-name-like db {:name-like "%zz%"}))

  (exsv (characters/characters-by-ids-specify-cols-sqlvec
         {:ids [1 2]
          :cols ["name" "specialty"]}))
  (ex (characters/characters-by-ids-specify-cols db
                                                 {:ids [1 2]
                                                  :cols ["name" "specialty"]})))

(defn -main []

  (println "\n\"The Princess Bride\" HugSQL Example App\n\n")

  (drop-tables) ;; if exists!
  (create-tables)
  (inserts)
  (updates)
  (selects)
  (deletes)
  (drop-tables)

  (println "\n\nTHE END\n"))
