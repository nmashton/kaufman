;; Auxiliary functions, assisting with the text-wrangling transformations
;; defined elsewhere.
(ns kaufman-justeson.functions
  (:use [clojure.string :only [split join]]))

(defn split-seq
  "A function for producing 'splitting' transducers. These partition
  a sequence by a predicate, then drop those parts that match the predicate."
  [pred]
  (comp
    (partition-by pred)
    (filter #(not (pred (first %))))))

;; Several specific variants of this.
(def split-on-xx
  (split-seq #(.startsWith % "xxxxx")))
(def split-on-eq
  (split-seq #(.startsWith % "=====")))
(def split-on-blanks
  (split-seq #(empty (.trim %))))

(defn clean-p-key
  "A function for cleaning up the values of the top-level semantic block keys."
  [s]
  (.trim (first (re-seq #"[^\%]+" s))))

(defn xx-line-semantics
  "Takes a line (the first line to come after the xx-delimiter)
  and returns the key it contains if it's there or a gensymmed
  value if it's not."
  [x]
  (let [pat (re-seq #"^\s{30,}\w+" x)]
    (if pat
      (.trim (first pat))
      (gensym "xx__"))))

(defn split-with-meta
  "Helper function to generate functions that split lines
  and push metadata down into the results."
  [transducer meta-key meta-fn]
  (fn [block]
    (eduction
      (comp
        transducer
        (map (fn [lines]
              (with-meta
                lines
                (into
                  (meta block)
                  {meta-key (meta-fn lines)})))))
      block)))

(def handle-xx
  "A function to perform the parsing process on blocks delimited by
  xx-lines."
  (split-with-meta
    split-on-xx
    :x-block
    #(xx-line-semantics (first %))))

(def handle-eq
  "A function to perform the parsing process on blocks delimited
  by ==-lines."
  (split-with-meta
    split-on-eq
    :eq-block
    (fn [_] (gensym "eq__"))))

(def handle-blanks
  "A function to perform the parsing process on blocks delimited
  by blanks."
  (split-with-meta
    split-on-blanks
    :space-block
    (fn [_] (gensym "space__"))))

(defn map-with-meta
  "Helper function to generate transducers that apply functions
  to lines and push down metadata."
  [f]
  (map
    (fn [lines]
      (with-meta
        (f lines)
        (meta lines)))))

(defn eliminable?
  "Identifies a string as crud that interferes with the creation of root maps.
  This is really just a dumping-ground for bad parts of the dictionary..."
  [s]
  (or
   (seq (re-seq #"^\s{30,}\w+" s))
   (.startsWith s "cf.")
   (.startsWith s "NOTHING MORE FOUND")
   (.startsWith s "? ")
   (.startsWith s "[")
   (.startsWith s "NOTE:")
   (.startsWith s "NOT IN")
   (.startsWith s "#tahb")
   (= "     pCh       *b'ahk'ut                      s         // fright, fear                           [K&N 41] " s)
   (.startsWith s "TZU ajo.b'e < **7ajaw.b'e")
   (.startsWith s "KCH ajaw.a.x < **7ajaw.a")
   (re-seq #"\[more to come\]" s)))
  ;  (.startsWith s "KAQ ajo7")
  ;  (.startsWith s "TZU ajo.b'e")
  ;  (.startsWith s "KCH ajaw.a.x")))

(defn root?
  "Identifies a string as a root header."
  [s]
  (not (or
        (empty? (re-seq #"^\S+" s))
        (eliminable? s))))

(defn divide-data-chunk
  "The function which splits a line into its component parts.
  Pretty simple, as these are mercifully regular."
  [line]
  (let [[prefix rest] (split-at 5 line)
        [language rest] (split-at 10 rest)
        [lexeme rest] (split-at 31 rest)
        [code rest] (split-at 10 rest)
        [gloss rest] (split-at 42 rest)
        source rest
        [t-prefix t-language t-lexeme t-code t-gloss t-source]
        (map #(.trim (join "" %)) [prefix
                                   language
                                   lexeme
                                   code
                                   gloss
                                   source])]
    {:prefix t-prefix
     :language t-language
     :lexeme t-lexeme
     :code t-code
     :gloss t-gloss
     :source t-source}))

(defn divide-data-chunk-with-meta
  [meta]
  #(with-meta
    (divide-data-chunk %)
    meta))

(defn to-csv
  "Converts the processed list of lexemes into a format suitable
  for CSV output."
  [lexemes]
  (let [lxs-meta (map #(into {} [(meta %) %]) lexemes)
        cols (keys (first lxs-meta))]
    (cons
      cols
      (map
        (fn [lexeme]
          (map
            #(% lexeme)
            cols))
        lxs-meta))))
