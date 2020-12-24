(defproject tablecloth.time "0.1.0-SNAPSHOT"
  :description "Time series manipulation library built on top of tablecloth"
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [scicloj/tablecloth "5.00-beta-21"]
                 [scicloj/notespace "3-alpha3-SNAPSHOT"]
                 [aerial.hanami "0.12.4"]]
  :repl-options {:init-ns tablecloth.time})
