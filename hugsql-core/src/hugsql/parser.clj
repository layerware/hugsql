(ns hugsql.parser
  (:require [clojure.string :as string]
            [clojure.tools.reader.reader-types :as r]))

(defn- parse-error
  ([rdr msg]
   (parse-error rdr msg {}))
  ([rdr msg data]
   (if (r/indexing-reader? rdr)
     (throw
      (ex-info
       (str msg " line: " (r/get-line-number rdr)
            ", column: " (r/get-column-number rdr))
       (merge data
              {:line   (r/get-line-number rdr)
               :column (r/get-column-number rdr)})))
     (throw (ex-info msg (merge data {:error :parse-error}))))))

(defn- sb-append
  [^StringBuilder sb ^Character c]
  (doto sb (.append c)))

(defn- whitespace? [^Character c]
  (when c
    (Character/isWhitespace ^Character c)))

(defn- symbol-char?
  [c]
  (boolean (re-matches #"[\pL\pM\pS\d\_\-\.\+\*\?\:\/%]" (str c))))

(defn- skip-ws-to-next-line
  "Read from reader until a non-whitespace or newline char is encountered."
  [rdr]
  (loop [c (r/peek-char rdr)]
    (when (and (whitespace? c)
               (not (= \newline c)))
      (r/read-char rdr)
      (recur (r/peek-char rdr)))))

(defn- skip-to-next-line
  "Read from reader until a new line is encountered.
   Reads (eats) the encountered new line."
  [rdr]
  (loop [c (r/read-char rdr)]
    (when (and c (not (= \newline c)))
      (recur (r/read-char rdr)))))

(defn- skip-to-chars
  "Read from reader until the two chars `c1 and `c2` are encountered.
   Read (eat) the encountered chars."
  [rdr c1 c2]
  (loop [rc (r/read-char rdr)
         pc (r/peek-char rdr)]
    (if (or (nil? rc) (nil? pc)
            (and (= rc c1) (= pc c2)))
      (do (r/read-char rdr) nil) ; read last peek char off, return nil
      (recur (r/read-char rdr) (r/peek-char rdr)))))

(defn- read-to-char
  "Read and return a string up to the encountered char `c`.
   Does not read the encountered character."
  [rdr c]
  (loop [s (StringBuilder.)
         pc (r/peek-char rdr)]
    (if (or (nil? pc) (= c pc))
      (str s)
      (recur (sb-append s (r/read-char rdr))
             (r/peek-char rdr)))))

(defn- read-to-chars
  "Read and return a string up to the encountered chars `c1` and `c2`.
   Does not read the encountered characters"
  [rdr c1 c2]
  (loop [s (StringBuilder.)
         rc (r/read-char rdr)
         pc (r/peek-char rdr)]
    (if (or (nil? rc) (nil? pc)
            (and (= c1 rc) (= c2 pc)))
      (do (r/unread rdr rc) (str s))
      (recur (sb-append s rc)
             (r/read-char rdr)
             (r/peek-char rdr)))))

(defn- read-keyword
  [rdr]
  (loop [result {}
         s  (StringBuilder.)
         rc (r/read-char rdr)
         pc (r/peek-char rdr)]
    (let [pgcast? (and (= \: rc) (= \: pc))]
      (cond
        (or (nil? rc) (nil? pc) pgcast? (not (symbol-char? pc)))
        ;; We're done.
        (do
          (when pgcast? (r/unread rdr rc))
          (let [s (str (if pgcast? s (sb-append s rc)))]
            (if (> (count s) 0)
              (assoc result :name s)
              (parse-error rdr (str "Incomplete keyword :" s)))))

        (= \: rc)
        ;; This is the end of the type specification.
        (recur (assoc result :type (str s))
               (StringBuilder.)
               (r/read-char rdr)
               (r/peek-char rdr))

        (= \/ rc)
        ;; This is the end of the namespace.
        (recur (assoc result :namespace (str s))
               (StringBuilder.)
               (r/read-char rdr)
               (r/peek-char rdr))

        :else
        (recur result
               (sb-append s rc)
               (r/read-char rdr)
               (r/peek-char rdr))))))

(defn- sing-line-comment-start?
  [c rdr]
  (and c (= \- c) (= \- (r/peek-char rdr))))

(defn- mult-line-comment-start?
  [c rdr]
  (and c (= \/ c) (= \* (r/peek-char rdr))))

(defn- sql-quoted-start?
  [c]
  (contains? #{\' \"} c))

(defn- sql-unmatched-quoted?
  [c]
  (contains? #{\' \"} c))

(defn- pg-type-cast-start?
  [rdr c]
  (and (= \: c) (= \: (r/peek-char rdr))))

(defn- escape-start?
  [rdr c]
  (let [p (r/peek-char rdr)]
    (and (= \\ c) (or (= \: p) (= \\ p)))))

(defn- hugsql-param-start?
  [c]
  (= \: c))

(defn- values-vector
  [s]
  (vec (remove string/blank?
               (string/split s #"\s+"))))

(defn- read-sing-line-header
  [rdr]
  (let [_   (r/read-char rdr) ; eat colon (:)
        key (-> rdr read-keyword :name keyword)
        line (read-to-char rdr \newline)
        values (if (= key :doc)
                 [(string/trim line)]
                 (values-vector line))]
    (skip-to-next-line rdr)
    {key values}))

(defn- read-mult-line-header
  [rdr]
  (let [_   (r/read-char rdr) ; eat colon (:)
        key (-> rdr read-keyword :name keyword)
        lines (read-to-chars rdr \* \/)
        _     (skip-to-chars rdr \* \/)
        values (if (= key :doc)
                 [(string/trim lines)]
                 (values-vector lines))]
    (skip-to-next-line rdr)
    {key values}))

(defn- read-sing-line-expr
  [rdr]
  (let [_    (r/read-char rdr) ; eat ~
        expr (string/trim (read-to-char rdr \newline))]
    [expr :end]))

(defn- read-mult-line-expr
  [rdr]
  (let [_ (r/read-char rdr) ; eat ~
        expr (string/trim (read-to-chars rdr \* \/))
        _    (skip-to-chars rdr \* \/)
        end? (= \~ (last expr))
        expr (if end? (string/trim (string/join "" (butlast expr))) expr)
        sign (if end? :end :cont)]
    (if (string/blank? expr) [sign] [expr sign])))

(defn- read-mult-line-hint
  [rdr]
  (let [_ (r/read-char rdr) ; eat +
        hint (read-to-chars rdr \* \/)
        _    (skip-to-chars rdr \* \/)]
    (str "/*+" hint "*/")))

(defn- read-sing-line-comment
  [rdr]
  (r/read-char rdr) ; eat second dash (-) of comment start
  (skip-ws-to-next-line rdr)
  (condp = (r/peek-char rdr)
    \: (read-sing-line-header rdr)
    \~ (read-sing-line-expr rdr)
    (skip-to-next-line rdr)))

(defn- read-mult-line-comment
  [rdr]
  (r/read-char rdr) ; eat second comment char (*)
  (skip-ws-to-next-line rdr)
  (condp = (r/peek-char rdr)
    \: (read-mult-line-header rdr)
    \~ (read-mult-line-expr rdr)
    \+ (read-mult-line-hint rdr)
    (skip-to-chars rdr \* \/)))

(defn- read-sql-quoted
  [rdr c]
  (let [quot c]
    (loop [s (sb-append (StringBuilder.) c)
           c (r/read-char rdr)]
      (condp = c
        nil    (parse-error rdr "SQL String terminated unexpectedly with EOF")
        quot  (let [pc (r/peek-char rdr)]
                (if (and pc (= pc quot) (not (= c pc)))
                  (recur (sb-append s c) (r/read-char rdr))
                  (str (sb-append s c))))
        ;; else
        (recur (sb-append s c) (r/read-char rdr))))))

(defn- read-hugsql-param
  [rdr]
  (let [{:keys [name namespace type]} (read-keyword rdr)]
    {:type (keyword (or type "v"))
     :name (if namespace
             (keyword namespace name)
             (keyword name))}))

(defn parse
  "Parse hugsql SQL string `sql` and return
   sequence of statement definitions
   of the form:

   ```
   {:hdr {:name   [\"my-query\"]
          :doc    [\"my doc string\"]
          :command [\":?\"]
          :result [\":1\"]
          :file \"sql/queries.sql\"
          :line 12}
    :sql [\"select * from emp where id = \"
          {:type :v :name :id}]}
   ```

   Throws `clojure.lang.ExceptionInfo` on error."
  ([sql] (parse sql {}))
  ([sql {:keys [no-header file]}]
   (if (string/blank? sql)
     (throw (ex-info "SQL is empty" {}))
     (let [sql (string/replace sql "\r\n" "\n")
           rdr (r/source-logging-push-back-reader sql)
           nsb #(StringBuilder.)]
       (loop [hdr {}
              sql []
              sb  (nsb)
              all []]
         (let [c (r/read-char rdr)]
           (cond

             ;; end of string, so return all, filtering out empty
             (nil? c)
             (vec
              (remove #(and (empty? (:hdr %))
                            (or (empty? (:sql %))
                                (and
                                 (every? string? (:sql %))
                                 (string/blank? (string/join (:sql %))))))
                      (conj all
                            {:hdr hdr
                             :sql (filterv seq (conj sql (string/trimr sb)))})))

             ;; SQL comments and hugsql header comments
             (or
              (sing-line-comment-start? c rdr)
              (mult-line-comment-start? c rdr))
             (if-let [x (if (sing-line-comment-start? c rdr)
                          (read-sing-line-comment rdr)
                          (read-mult-line-comment rdr))]
               ;; hdr was read from comment
               (cond
                 (map? x)
                 ;; if sql is active, then new hdr section
                 (if (or (> (.length ^StringBuilder sb) 0) (empty? hdr))
                   (recur (merge x {:file file :line (max 1 (dec (r/get-line-number rdr)))})
                          []
                          (nsb)
                          (conj all
                                {:hdr hdr
                                 :sql (filterv seq (conj sql (str sb)))}))
                   (recur (merge hdr x) sql sb all))
                 ;; hint
                 (string? x)
                 (recur hdr sql (sb-append sb x) all)
                 :else
                 ;; clj expr was read from comment
                 (recur hdr (conj sql (str sb) x) (nsb) all))
               (recur hdr sql sb all))


             ;; quoted SQL (which cannot contain hugsql params,
             ;; so we consider them separately here before


             (sql-quoted-start? c)
             (recur hdr sql (sb-append sb (read-sql-quoted rdr c)) all)

             ;; missing an SQL quote
             (sql-unmatched-quoted? c)
             (parse-error rdr (str "Unmatched SQL quote: " c))

             ;; postgresql :: type cast is not hugsql param, so skip double-colon
             (pg-type-cast-start? rdr c)
             (recur hdr sql (sb-append (sb-append sb c) (r/read-char rdr)) all)

             ;; escaped colon
             (escape-start? rdr c)
             (recur hdr sql (sb-append sb (r/read-char rdr)) all)

             ;; hugsql params
             (hugsql-param-start? c)
             (recur hdr
                    (vec (filter seq
                                 (conj sql (str sb) (read-hugsql-param rdr))))
                    (nsb)
                    all)

             ;; all else is SQL
             :else
             (if (and (not (string/blank? sb)) (empty? hdr) (not no-header))
               (parse-error rdr "Encountered SQL with no hugsql header")
               (recur hdr sql (sb-append sb c) all)))))))))