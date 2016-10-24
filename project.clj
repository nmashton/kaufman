(defproject kaufman "0.0.1"
  :description "Kaufman & Justeson 2003 processor"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "4.0.2"]
                 [prismatic/plumbing "0.5.3"]]
  :dev-dependencies [[lein-marginalia "0.9.0"]
                     [proto-repl "0.3.1"]]
  :jvm-opts ["-Xms256m" "-Xmx1024m"]
  :plugins [[lein-marginalia "0.9.0"]])
