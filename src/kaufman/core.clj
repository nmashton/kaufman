;; Threads the data file through the transformations defined in [kaufman.actions](#kaufman.actions).
;; Produces both a map mirroring the hierarchical structure of the file, `processed`,
;; and a flat list of maps suitable for importing into a relational database, `relational`.
(ns kaufman.core
  (:use [kaufman.actions])
  (:use [cheshire.core]))

;; The core sequence of transformations.
;; To understand what these do, consult [kaufman.actions](#kaufman.actions).
(def processed
  (->
    "./data/kaufman-cleaned.txt"
    (get-raw-data)
    (strip-pagenums-footers)
    (strip-percents)
    (group-by-percent-delimiters)
    (split-on-xx-delimiters)
    (group-by-xx-keys)
    (flatten-spurious-sequences)
    (split-on-eq-delimiters)
    (eliminate-garbage)
    (eliminate-more-garbage)
    (partition-by-root-lines)
    (group-by-root-lines)
    (split-on-blank-lines)
    (turn-lines-into-maps)
    (eliminate-blank-lexemes)
    (convert-pseudo-maps)))

;; The "relational" encoding of `processed`.
(def relational
  (convert-to-relational processed))

;; We can write the result to a JSON file, using Cheshire.
(comment
  (spit "./data/kaufman-processed.json"
        (generate-string processed {:pretty false})))