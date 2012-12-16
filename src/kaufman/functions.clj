;; Auxiliary functions, assisting with the text-wrangling transformations defined elsewhere.
(ns kaufman.functions
	(:use [clojure.string :only [split join]]))

;; A pair of functions which, together, efficiently turn a file into lines.
;; It's necessary to use this over `slurp` because the file in question is quite big.
(defn reduce-file [filename f acc]
  (with-open [rdr (java.io.BufferedReader.
                    (java.io.InputStreamReader.
                      (java.io.FileInputStream. filename)
                      "UTF-8"))]
    (reduce f acc (line-seq rdr))))
(defn get-lines [filename]
  (reduce-file filename conj []))

;; This function is like filter, but it tests *n*-length subsequences of the sequence.
;; A trailing subseq of length < *n* is permitted to continue to exist.
;; This function is very, very slow -- but wrapping its return value in `lazy-seq`
;; ameliorates this to some extent.
(defn filter-n-lazy [n pred xs]
  (lazy-seq
    (cond
      (empty? xs) xs
      (< (count xs) n) xs
      :else
      (if (pred (take n xs))
        (cons (first xs) (filter-n-lazy n pred (rest xs)))
        (filter-n-lazy n pred (drop n xs))))))

;; Predicates to identify lines that are either page numbers or footers.
(defn footer? [line]
  (.startsWith line "Kaufman:  preliminary Mayan Etymological Dictionary"))
(defn pagenumber? [line]
  (re-seq #"^[0-9]+\s*$" line))

;; The five-place filter that detects pagenum-footer sequences.
(defn not-pagenumber-footer-sequence [[l1 l2 l3 l4 l5]]
  (not (and	(empty? l1)
             (pagenumber? l2)
             (empty? l3)
             (footer? l4)
             (empty? l5))))


;; The `pairs` function helps construct maps from partitioned sequences.
;; It turns a sequence into a sequence of length-2 sequences.
;; ... as it turns out, as @tsrmlis points out to me, `partition` can do this!
(defn pairs [xs]
  (lazy-seq
    (if (empty? xs) xs
      (cons [(first xs) (second xs)] (pairs (drop 2 xs))))))

;; A generic "splitting" function. Partitions a sequence by
;; a predicate, then drops those parts that match the predicate.
(defn split-seq [pred xs]
  (filter #(not (pred (first %)))
          (partition-by pred xs)))

;; Several specific variants of this.
;; `split-seq` was defined by abstracting away from the ad-hoc versions of
;; these, each of which used to be defined separately. Oy!
(defn split-on-xx [vs]
  (split-seq #(.startsWith % "xxxxx") vs))
(defn split-on-eq [vs]
  (split-seq #(.startsWith % "=====") vs))
(defn split-on-blanks [vs]
  (split-seq #(empty? (.trim %)) vs))

;; A function for cleaning up the values of the top-level semantic block keys.
(defn clean-p-key [s]
  (.trim (first (re-seq #"[^\%]+" s))))

;; Takes a line (the first line to come after
;; the xx-delimiter) and returns the "key" it contains if it's there or
;; a gensymmed value if it's not.
(defn xx-line-semantics [x]
  (let [pat (re-seq #"^\s{30,}\w+" x)]
    (if pat
      (.trim (first pat))
      (str (gensym "xx__")))))

;; Applies `xx-line-semantics` to the first
;; line in a sequence of strings. This is intended to apply to the partitioned
;; sequences yielded up by `split-on-xx`.
(defn xx-block-semantics [xs]
  (xx-line-semantics (first xs)))


;; This predicate identifies a string as a root header.
;; Yes, it is especially crude.
;; No, I can't see any way to do it any better.
(defn root? [s]
  (and
    (not (empty? (re-seq #"^\S+" s)))
    (not (.startsWith s "cf."))
    (not (.startsWith s "? "))))

;; This function takes a sequence of lines and partitions it by its
;; putative root header. It tries to eliminate empty lines or other
;; dreck that may precede root headers in the resulting partitioned
;; sequence, allowing it to be more smoothly transformed into
;; pairs and thus a map (ala Step 3 in [kaufman.actions](#kaufman.actions)).
(defn partition-by-root [lines]
  (let [partitioned (partition-by root? lines)
        testitem (first (first partitioned))]
    (if (or
          (empty? (.trim testitem))
          (.startsWith testitem "     "))
      (rest partitioned)
      partitioned)))

;; The function which splits a line into its component parts is pretty
;; simple, since these lines are mercifully regular.
(defn divide-data-chunk [line]
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

;; The following functions simplify the manipulation of deeply embedded
;; maps. Thanks to @tsrmlis for giving me the impetus to fix the hideous nest
;; of maps and anonymous functions I had been employing to do this!
(defn map-vals [f coll]
  (map
    (fn [[k v]]
      [k (f v)])
    coll))
(defn map-vals-2 [f coll]
  (map 
    (fn [[k1 v1]]
      [k1 
       (map 
         (fn [[k2 v2]]
           [k2 (f v2)])
         v1)])
    coll))

;; I'm aware that most of these functions are needlessly repetitive and could
;; be rewritten as iterations of `map-vals` ... but I'm too lazy to think through
;; how this could be done, at the moment.

;; ``map-vals-mms`` and the functions following it operate with a mixture of maps and simple sequences.
;; The names of the functions are mnemonic: "mms" indicates that the nesting
;; is map-map-sequence, "mmsm" map-map-sequence-map, etc.
(defn map-vals-mms [f coll]
  (map 
    (fn [[k1 v1]]
      [k1 
       (map 
         (fn [[k2 v2]]
           [k2
            (map
              (fn [es]
                (f es)) 
              v2)]) 
         v1)]) 
    coll))
(defn map-vals-mmsm [f coll]
  (map 
    (fn [[kp vp]]
      [kp 
       (map 
         (fn [[kx vx]]
           [kx 
            (map 
              (fn [es]
                (map 
                  (fn [[kr vr]]
                    [kr (f vr)])
                  es))
              vx)])
         vp)])
    coll))
(defn map-vals-mmsms [f coll]
  (map 
    (fn [[kp vp]]
      [kp 
       (map 
         (fn [[kx vx]]
           [kx 
            (map 
              (fn [es]
                (map 
                  (fn [[kr vr]]
                    [kr
                      (map
                        (fn [bs]
                          (f bs))
                        vr)])
                  es))
              vx)])
         vp)])
    coll))