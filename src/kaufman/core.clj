;; Threads the data file through the transformations
;; defined in [kaufman.transducers](#kaufman.transducers).
;; Produces a list of lexemes tagged with metadata.
(ns kaufman.core
  (:use [kaufman.transducers]))

(def processed-tr
  (with-open [rdr (clojure.java.io/reader "./data/kaufman-footers-stripped.txt")]
    (doall
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
        (line-seq rdr)))))
