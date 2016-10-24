;; Threads the data file through the transformations defined in [kaufman.actions](#kaufman.actions).
;; Produces both a map mirroring the hierarchical structure of the file, `processed`,
;; and a flat list of maps suitable for importing into a relational database, `relational`.
(ns kaufman.core
  (:use [kaufman.actions]))

;; The core sequence of transformations.
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
      group-by-root-lines-tr
      split-on-blank-lines-tr
      turn-lines-into-maps-tr
      remove-blank-lexemes-tr)
    (get-raw-data "./data/kaufman-footers-stripped.txt")))
