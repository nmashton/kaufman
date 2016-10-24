;; Auxiliary functions, assisting with the text-wrangling transformations defined elsewhere.
(ns kaufman.functions
  (:use [clojure.string :only [split join]])
  (:require [plumbing.map :as pl]))

;; A pair of functions which, together, efficiently turn a file into lines.
;; It's necessary to use this over `slurp` because the file in question is quite big.
(defn- reduce-file
  [filename f acc]
  (with-open [rdr (java.io.BufferedReader.
                   (java.io.InputStreamReader.
                    (java.io.FileInputStream. filename)
                    "UTF-8"))]
    (reduce f acc (line-seq rdr))))
(defn get-lines
  "Efficiently read in a file from a filename."
  [filename]
  (reduce-file filename conj []))

(defn filter-n-lazy
  "This function is like filter, but it tests *n*-length subsequences of the sequence.
  A trailing subseq of length < *n* is permitted to continue to exist.
  This function is very, very slow -- but wrapping its return value in `lazy-seq`
  ameliorates this to some extent."
  [n pred xs]
  (lazy-seq
   (cond
    (empty? xs) xs
    (< (count xs) n) xs
    :else
    (if (pred (take n xs))
      (cons (first xs) (filter-n-lazy n pred (rest xs)))
      (filter-n-lazy n pred (drop n xs))))))

;; Predicates to identify lines that are either page numbers or footers.
(defn footer?
  [line]
  (.startsWith line "Kaufman:  preliminary Mayan Etymological Dictionary"))
(defn pagenumber?
  [line]
  (re-seq #"^[0-9]+\s*$" line))

;; The five-place filter that detects pagenum-footer sequences.
(defn not-pagenumber-footer-sequence
  [[l1 l2 l3 l4 l5]]
  (not (and  (empty? l1)
            (pagenumber? l2)
            (empty? l3)
            (footer? l4)
            (empty? l5))))

(defn half-flatten
  "De-sequentializes the first element in a sequence of sequences.
  Also makes damn sure that it's a singleton (by dropping everything
  except its first element)."
  [xs]
  (cons (first (first xs)) (rest xs)))

(defn partition-to-map
  "Helps construct maps from partitioned sequences.
  Turns a partitioned sequence into a map."
  [xs]
  (if (or (odd? (count xs))
          (empty? xs))
    {}
    (->> xs
         (partition 2)
         (map half-flatten)
         (map #(apply vector %))
         (into {}))))

(defn map-keys
  "Applies a function of one argument to all the keys
  in a map, returning the map resulting from replacing all
  the keys with the values of the function on those keys."
  [f m]
  (into {} (map (fn [[k v]]
                  [(f k) v]) m)))

(defn apply-leaves
  "Applies a function to the leaves of a deeply nested map."
  [f m]
  (->> m
       pl/flatten
       (map (fn [[ks vs]] [ks (f vs)]))
       pl/unflatten))

(defn leaves
  "Get the leaves of a nested map."
  [m]
  (map second (pl/flatten m)))

(defn arbitrary-map
  "Turns a sequence into a map by assigning a randomly generated
  key to each item in the sequence."
  [k xs]
  (->> xs
       (map #(vector (str k (gensym)) %))
       (into {})))

(defn split-seq
  "A generic 'splitting' function. Partitions a sequence by
  a predicate, then drops those parts that match the predicate."
  [pred xs]
  (->> xs
       (partition-by pred)
       (filter #(not (pred (first %))))))

(defn split-seq-tr
  [pred]
  (comp
    (partition-by pred)
    (filter #(not (pred (first %))))))

;; Several specific variants of this.
;; `split-seq` was defined by abstracting away from the ad-hoc versions of
;; these, each of which used to be defined separately. Oy!
(defn split-on-xx
  [vs]
  (split-seq #(.startsWith % "xxxxx") vs))
(def split-on-xx-tr
  (split-seq-tr #(.startsWith % "xxxxx")))
(defn split-on-eq
  [vs]
  (split-seq #(.startsWith % "=====") vs))
(def split-on-eq-tr
  (split-seq-tr #(.startsWith % "=====")))
(defn split-on-blanks
  [vs]
  (split-seq #(empty? (.trim %)) vs))
(def split-on-blanks-tr
  (split-seq-tr #(empty (.trim %))))

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

(defn handle-xx
  [higher-meta]
  (comp
    split-on-xx-tr
    (map
      (fn [lines]
        (with-meta
          lines
          (into
            higher-meta
            {:x-block (xx-line-semantics (first lines))}))))))

(defn handle-eq
  [higher-meta]
  (comp
    split-on-eq-tr
    (map
      (fn [lines]
        (with-meta
          lines
          (into
            higher-meta
            {:eq-block (gensym "eq__")}))))))


(defn handle-blanks
  [higher-meta]
  (comp
    split-on-blanks-tr
    (map
      (fn [lines]
        (with-meta
          lines
          (into
            higher-meta
            {:space-block (gensym "space__")}))))))

(defn xx-block-semantics
  "Applies `xx-line-semantics` to the first line in a sequence of strings.
  This is intended to apply to the partitioned sequences yielded up by `split-on-xx`."
  [xs]
  (xx-line-semantics (first xs)))


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
   (.startsWith s "KCH ajaw.a.x < **7ajaw.a")))
  ;  (.startsWith s "KAQ ajo7")
  ;  (.startsWith s "TZU ajo.b'e")
  ;  (.startsWith s "KCH ajaw.a.x")))

(defn root?
  "Identifies a string as a root header."
  [s]
  (not (or
        (empty? (re-seq #"^\S+" s))
        (eliminable? s))))

(defn fix-root-partition
  "Cleans up the result of partitioning by root.
  This includes eliminating crap coming before the root
  and gluing on an ad-hoc root if there isn't one."
  [xs]
  (let [filtered (drop-while (comp empty? (partial filter root?)) xs)]
    (if (seq? filtered)
      filtered
      (cons [(str "No root " (gensym))] xs))))

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
