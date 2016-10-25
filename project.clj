(defproject kaufman-justeson "0.0.1"
  :description "Kaufman & Justeson 2003 processor"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.csv "0.1.3"]]
  :dev-dependencies [[lein-marginalia "0.9.0"]]
  :jvm-opts ["-Xms256m" "-Xmx1024m"]
  :plugins [[lein-marginalia "0.9.0"]]
  :main kaufman-justeson.core)
