(defproject tablecloth.time "0.1.0-SNAPSHOT"
  :description "Time series manipulation library built on top of tablecloth"
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [scicloj/tablecloth "5.00-beta-28"]
                 [tick "0.4.27-alpha"]
                 [scicloj/notespace "3-alpha3-SNAPSHOT"]
                 [aerial.hanami "0.12.4"]]
  :plugins [[lein-cljfmt "0.7.0"]
            [jonase/eastwood "0.3.14"]]
  :aliases {"lint" ["do"
                    ["cljfmt" "check"]
                    ["eastwood" "{:source-paths [\"src\"]}"]]})
