;; Threads the data file through the transformations defined in [kaufman.actions](#kaufman.actions).
;; Produces both a map mirroring the hierarchical structure of the file, `processed`,
;; and a flat list of maps suitable for importing into a relational database, `relational`.
(ns kaufman.core
  (:use [kaufman.actions])
  (:use [cheshire.core]))

;; The core sequence of transformations.
;; To understand what these do, consult [kaufman.actions](#kaufman.actions).
; (def processed
;   (->
;    "./data/kaufman-cleaned.txt"
;    get-raw-data
;    strip-pagenums-footers
;    strip-percents
;    group-by-percent-delimiters
;    split-on-xx-delimiters
;    group-by-xx-keys
;    flatten-spurious-sequences
;    split-on-eq-delimiters
;    eliminate-garbage
;    eliminate-more-garbage
;    eliminate-even-more-garbage
;    group-by-root-lines
;    split-on-blank-lines
;    turn-lines-into-maps
;    remove-blank-lexemes))

(def processed-tr
  (eduction
    (comp
      strip-percents-tr
      group-with-semantic-field-block-tr
      split-on-xx-delimiters-tr
      split-on-eq-delimiters-tr
      eliminate-garbage-tr
      eliminate-more-garbage-tr
      eliminate-even-more-garbage-tr
      group-by-root-lines-tr)
    (get-raw-data "./data/kaufman-footers-stripped.txt")))

;; The "relational" encoding of `processed`.
; (def relational
;   (convert-to-relational processed))

;; We can write the result to a JSON file, using Cheshire.
; (comment
;   (spit "./data/kaufman-processed.json"
;         (generate-string processed {:pretty false})))
