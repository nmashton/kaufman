;; Functions for effecting the sequence of transformations that turns the
;; data into something useful. The data will be threaded through all these in [kaufman.core](#kaufman.core).
(ns kaufman.actions
  (:use [kaufman.functions]))
;; The goal of these transformations is to parse the data file into a map
;; which preserves the hierarchical structure contained in (or at least strongly suggested by)
;; the data file itself.

;; # Step 1. Get the file.

;; Grab the data, using the efficient reader function `get-lines` defined in [kaufman.functions](#kaufman.functions).
(defn get-raw-data [name]
  (get-lines name))


;; # Step 2. Strip page numbers and footers from the data.

;; Deleting page numbers and footers means not just deleting the individual lines
;; that contain page numbers and footers but deleting the five-line sequences
;; which contain those individual lines, as well as flanking and separating blanks.
;; Deleting the entire subsequence rather than its component parts
;; is necessary because many semantically meaningful
;; chunks of the text are delimited by blank spaces. Indeed, the five-line
;; pagenum-footer sequences sometimes interrupt those chunks.



;; Strip pagenum-footer sequences from the data.
(defn strip-pagenums-footers [data]
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


;; Strip lines that consist of nothing but %s.
;; Such lines, unlike lines that just start with %%, are not meaningful and can be completely ignored.
(defn strip-percents [data]
  (filter
    (fn [x] (not (re-seq #"^\%+\s*$" x)))
    data))

;; Partition the data by lines that start with %%, turning the
;; result into a map, as described above.
(defn group-by-percent-delimiters [data]
  (map #(update-in % [0] first)
       (pairs (partition-by #(.startsWith % "%% ") data))))

;; Note that we do not create a real map, just a sequence of `[k v]` pairs.
;; This is because we will be, at all points, treating the map as a sequence
;; and iterating over its component `[k v]` pairs. So there's really no point
;; in creating a bona fide map until the very last transformation.


;; # Step 4. Parse next level of structure: xx-delimiters.

;; From hereon in, processing the data consists of manipulating values
;; inside the map created in Step 3. This swiftly gets regrettably hairy,
;; involving pretty bewildering nested operations which have been
;; partly abstracted away via the family of `map-vals` functions
;; defined in [kaufman.functions](#kaufman.functions).
;; *If there is a better way to do this, I sincerely wish to know what it is.*

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

;; The resulting partitioned sequence can be turned into a map, much
;; like in Step 3, since at least *some* of the xx-delimited chunks have
;; a natural "key" in the form of an identifier extractable from the line
;; that follows the xx-delimiter.
;; Some, of course, do not. So we need a function which detects the key
;; when it is present—and generates an arbitrary unique key when it's not.


;; Apply splitting and grouping transformations to the sequences
;; of strings keyed by %%-delimiters.
(defn split-on-xx-delimiters [data]
  (map-vals split-on-xx data))

;; Here we start making use of the auxiliary functions defined in [kaufman.functions](#kaufman.functions)
;; that facilitate the manipulation of values inside embedded maps. Code using these functions is a little
;; opaque, but it is an improvement over the hideous shell of `(map (fn ...))`s that I had been using
;; before. (A remnant of this horror is still visible at the end of this namespace definition.)


;; To create a map from the result of applying `split-on-xx` to the
;; data, we use `group-by` to collect the partitioned sequences by the
;; result of calling xx-block-semantics on them. This creates a map
;; where the keys are the values yielded by xx-line-semantics.
(defn group-by-xx-keys [data]
  (map-vals #(group-by xx-block-semantics %) data))



;; Because partition-by has the side effect of putting things
;; into sequences, it's useful to strip off these spurious sequences
;; before we proceed.
(defn flatten-spurious-sequences [data]
  (map-vals-2 flatten data))



;; # Step 5. Parse next level of structure: ==-delimiters.

;; We now need to partition the blocks inside the value of `px-split` by the lines that consist
;; of equals-signs. These blocks denote sets of forms that share a root. But this
;; root is not in fact given in the text. Hence we do not create a new inner map here at all;
;; we just create an inner sequence.

;; Partition the innermost blocks of strings by ==-lines.
(defn split-on-eq-delimiters [data]
  (map-vals-2 split-on-eq data))


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

;; Weed out blank lines and remnants of xx-headers.
(defn eliminate-garbage [data]
  (map-vals-2 #(map (fn [e]
                      (if (or
                            (.startsWith (first e) "     ")
                            (empty? (.trim (first e))))
                        (rest e)
                        e)) %) data))

;; Weed out those parts of the structure that are
;; totally empty. If you don't do this now, it will completely
;; screw up everything very shortly down the road.
;; This came to light through painful trial and error.
(defn eliminate-more-garbage [data]
  (map-vals-2
    (fn [e] (filter #(not (empty? %)) e))
    data))

;; Create an inner map keyed by root headers,
;; more or less like in Step 3.
;; First part: partition the inner blocks of strings.
(defn partition-by-root-lines [data]
  (map-vals-mms
    partition-by-root
    data))

;; Second part: turn the result into a map.
(defn pair-root-lines [data]
  (map-vals-mms pairs data))
(defn group-by-root-lines [data]
  (map-vals-mms
    (fn [xs]
      (map #(update-in % [0] first) xs))
    (pair-root-lines data)))


;; # Step 7. Group the innermost sequence of lines into blank-delimited chunks.

;; This next parsing step is mercifully simple. As before, we split 
;; on the delimiter, here blank lines.

;; Split the innermost sequence of lines on blanks.
(defn split-on-blank-lines [data]
  (map-vals-mmsm split-on-blanks data))


;; # Step 8. Turn the remaining strings into "entry" maps.

;; The final step in parsing is to turn the lines, which are now divided up
;; in a maximally informative way, into maps representing their contents.

;; Turn the remaining strings into "entry" maps.
(defn turn-lines-into-maps [data]
  (map-vals-mmsms
    #(map divide-data-chunk %)
    data))

;; Filter the results to eliminate blanks lexemes.
(defn eliminate-blank-lexemes [data]
  (map-vals-mmsms
    (fn [b] (filter #(get % :lexeme) b))
    data))
;; Why do these happen? Mostly as a result of fields from preceding
;; lexemes spilling over onto the next line. Perhaps a future revision of
;; this script will detect these and amend the lines. But that will
;; be very difficult, and mercifully, not doing it doesn't screw things up
;; *so* badly.


;; # Conclusion

;; The final step: Transform all the pseudo-maps into actual maps.
(defn convert-pseudo-maps [data]
  (into {} 
        (map 
          (fn [[kp vp]]
            [kp 
             (into {} 
                   (map 
                     (fn [[kx vx]]
                       [kx 
                        (map 
                          (fn [es]
                            (into {} 
                                  es))
                          vx)])
                     vp))])
          data)))

;; And for lagnappe, let us now convert everything into a big sequence
;; of simple maps, suitable for porting into a relational database or the like.
(defn convert-to-relational [data]
  (flatten 
    (map 
      (fn [[kp vp]]
        (flatten 
          (map 
            (fn [[kx vx]]
              (flatten 
                (map 
                  (fn [es]
                    (flatten 
                      (map 
                        (fn [[kr vr]]
                          (flatten 
                            (map 
                              (fn [bs]
                                (map 
                                  (fn [h]
                                    (into 
                                      {:semantics-top kp
                                       :semantics-x kx
                                       :eq-block (str (gensym))
                                       :root kr
                                       :space-block (str (gensym))}
                                      h))
                                  bs))
                              vr)))
                        es)))
                  vx)))
            vp)))
data)))
;; There is, unfortunately, no abstracting away from the structure of this one,
;; so please forgive the lack of a helper function like `map-vals-mmsmss` here!