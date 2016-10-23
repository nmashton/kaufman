;; Functions for effecting the sequence of transformations that turns the
;; data into something useful. The data will be threaded through all these in [kaufman.core](#kaufman.core).
(ns kaufman.actions
  (:use [kaufman.functions])
  (:require [plumbing.map :as pl]))
;; The goal of these transformations is to parse the data file into a map
;; which preserves the hierarchical structure contained in (or at least strongly suggested by)
;; the data file itself.

;; # Step 1. Get the file.
(defn get-raw-data
  "Grab the data, using the efficient reader function `get-lines`
  defined in [kaufman.functions](#kaufman.functions)."
  [name]
  (get-lines name))


;; # Step 2. Strip page numbers and footers from the data.

;; Deleting page numbers and footers means not just deleting the individual lines
;; that contain page numbers and footers but deleting the five-line sequences
;; which contain those individual lines, as well as flanking and separating blanks.
;; Deleting the entire subsequence rather than its component parts
;; is necessary because many semantically meaningful
;; chunks of the text are delimited by blank spaces. Indeed, the five-line
;; pagenum-footer sequences sometimes interrupt those chunks.

(defn strip-pagenums-footers
  "Strip pagenum-footer sequences from the data."
  [data]
  (filter-n-lazy 5 not-pagenumber-footer-sequence data))


;; # Step 3. Parse the top level of hierarchical structure.

;; The remainder of the transformations applied to the data consist in
;; splitting (really, partitioning) the sequence of lines on certain "meaningful" line patterns
;; and turning the results into a map.

;; That is, we take a sequence like:
;;
;; -	`[h1 a b c h2 d e f]`
;;
;; And partition it into:
;;
;; -	`[[h1] [a b c] [h2] [d e f]]`
;;
;; From which we can create pairs:
;;
;; -	`[[[h1] [a b c]] [[h2] [d e f]]]`
;;
;; And from that a map:
;;
;; -	`{h1 [a b c] h2 [d e f]}`

;; We iterate this process within the inner values, creating a more and more
;; deeply embedded map.

;; The first step applies to lines that start with %%, which are
;; delimiters of top-level semantic categories. We break on these
;; lines and use their contents as keys in the resulting map.

(defn strip-percents
  "Strip lines that consist of nothing but %s.
  Such lines, unlike lines that just start with %%,
  are not meaningful and can be completely ignored."
  [data]
  (filter (fn [x] (not (re-seq #"^\%+\s*$" x)))
          data))

(def strip-percents-tr
  (filter
    #(not (re-seq #"^\%+\s*$" %))))

(defn group-by-percent-delimiters
  "Partition the data by lines that start with %%, turning the result
  into a map, as described above.

  At this point it's convenient to apply a cleaning function `clean-p-key`
  to strip off the meaningless crud that encloses the %%-keys."
  [data]
  (->> data
       (partition-by #(.startsWith % "%% "))
       partition-to-map
       (map-keys clean-p-key)))


;; # Step 4. Parse next level of structure: xx-delimiters.

;; From hereon in, processing the data consists of manipulating values
;; inside the map created in Step 3. Thanks to Prismatic's plumbing
;; library, this never has to get too hairy. We're just mapping onto
;; the leaves at every step.

;; Beneath the level of %%-delimiters, there is more structure to parse.
;; The next structural delimiter to split on is the line consisting of
;; xxs. Such a line marks a finer-grained semantic category.

;; Unlike lines starting with %%, xx-lines do not themselves contain
;; the semantic information we want to capture and use as a key. This
;; information is on the next line—when it exists, and crucially
;; it does not always exist!

;; In any case, because the xx-delimiters themselves are not meaningful,
;; we can partition on them and then filter them out (split the sequence
;; ala Python, in other words).

(defn split-on-xx-delimiters
  [data]
  (apply-leaves split-on-xx data))


;; The resulting partitioned sequence can be turned into a map, much
;; like in Step 3, since at least *some* of the xx-delimited chunks have
;; a natural "key" in the form of an identifier extractable from the line
;; that follows the xx-delimiter.

;; Some, of course, do not. So we need a function which detects the key
;; when it is present—and generates an arbitrary unique key when it's not.

;; To create a map from the result of applying `split-on-xx` to the
;; data, we use `group-by` to collect the partitioned sequences by the
;; result of calling `xx-block-semantics` on them. This creates a map
;; where the keys are the values yielded by `xx-line-semantics`
;; (which is used within `xx-block-semantics`).
(defn group-by-xx-keys
  [data]
  (apply-leaves #(group-by xx-block-semantics %) data))


;; Because partition-by has the side effect of putting things
;; into sequences, it's useful to strip off these spurious sequences
;; before we proceed.
(defn flatten-spurious-sequences
  [data]
  (apply-leaves flatten data))



;; # Step 5. Parse next level of structure: ==-delimiters.

;; We now need to partition the blocks inside the value of `px-split` by the lines that consist
;; of equals-signs. These blocks denote sets of forms that share a root. But this
;; root is not in fact given in the text. So we just create an arbitrary map
;; with keys generated by `gensym`.

;; Partition the innermost blocks of strings by ==-lines.
(defn split-on-eq-delimiters
  [data]
  (apply-leaves #(arbitrary-map "eq" (split-on-eq %)) data))


;; # Step 6. Parse next level of structure: root headers.

;; At the next level, blocks of content are delimited by lines indicating
;; the reconstructed roots that items in the block share—or by other, similar-looking identifiers.
;; These delimiting lines are *almost* identifiable by their not having any leading
;; whitespace. Unfortunately, that's about the only thing which all of the
;; identifiers share—and many other things in the data also look like that!
;; I've done my best to weed out lines from the data that would throw
;; off detection of root headers ... but inevitably a certain amount of
;; error enters the structure at this point. So it goes.

;; Breaking down the inner blocks by putative root headers proceeds more
;; or less like previous stages of parsing.
;; First, however, it is necessary to weed out blank lines and remnants
;; of xx-headers from the tops of blocks of content, as these will
;; foil our attempts to detect root headers if left unattended.

(defn eliminate-garbage
  "Weed out initial blanks and remnants of xx-headers."
  [data]
  (apply-leaves
   (fn [xs]
     (if (or
          (.startsWith (first xs) "     ")
          (empty? (.trim (first xs))))
       (rest xs)
       xs))
   data))

(defn eliminate-more-garbage
  "Weed out totally empty parts of the structure.
  This prevents problems shortly down the road."
  [data]
  (apply-leaves
   (fn [xs]
     (filter #(not (empty? (.trim %))) xs))
   data))

(defn eliminate-even-more-garbage
  "Perform yet another round of cleanups."
  [data]
  (apply-leaves
   #(filter (complement eliminable?) %)
   data))

(defn group-by-root-lines
  "Create an inner map keyed by root headers."
  [data]
  (->> data
       (apply-leaves (partial partition-by root?))
       (apply-leaves fix-root-partition)
       (apply-leaves partition-to-map)))


;; # Step 7. Group the innermost sequence of lines into blank-delimited chunks.

;; This next parsing step is mercifully simple. As before, we split
;; on the delimiter, here blank lines.

(defn split-on-blank-lines
  "Split the innermost sequence of lines on blanks."
  [data]
  (->> data
       (apply-leaves split-on-blanks)
       (apply-leaves (partial arbitrary-map "block-"))))


;; # Step 8. Turn the remaining strings into "entry" maps.

;; The final step in parsing is to turn the lines, which are now divided up
;; in a maximally informative way, into maps representing their contents.

(defn turn-lines-into-maps
  "Turn remaining strings into lexical entry maps."
  [data]
  (apply-leaves (partial map divide-data-chunk) data))

(defn remove-blank-lexemes
  "Filters out the blanks."
  [data]
  (apply-leaves (partial filter #(get % :lexeme)) data))


;; # Step 8.5. Create a "relational" representation.

;; The hierarchical structure resulting from our work isn't
;; actually useful for most purposes, faithful though it is to
;; the source. What we really want is often something "tabular",
;; where each data point consists of a set of fields with values.
;; These last functions create such a representation out of
;; the nested map.
(defn relationalize
  "Converts a flattened item from the data map into a flat map,
  suitable for importation into a relational database or the like."
  [[[kp kx ke kr kb] vs]]
  (map #(merge {:semantic-field-block kp
                :x-block kx
                :eq-block ke
                :root-block kr
                :space-block kb}
               %)
       vs))

(defn convert-to-relational
  "This converts everything into a big sequence
  of simple maps a la `relationalize`."
  [data]
  (->> data
       pl/flatten
       (map relationalize)
       flatten))
