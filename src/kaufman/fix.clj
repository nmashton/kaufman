(ns kaufman.fix
  (:use [cheshire.core])
  (:use [clojure.string :only [split join replace]]))

(def thealm
  (-> (slurp "./data/MAY.alm")
      (split #"\n")))

(def filtered
  (filter
    #(seq (re-find #"\S{2,}$" %)) thealm))