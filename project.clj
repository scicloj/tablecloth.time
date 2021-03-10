(defproject tablecloth.time "0.1.0-SNAPSHOT"
  :description "Time series manipulation library built on top of tablecloth"
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.10.2"]
                 [scicloj/tablecloth "5.04"]
                 [tick "0.4.27-alpha"]
                 [org.threeten/threeten-extra "1.5.0"]]

  :profiles {:dev
             {:dependencies [[scicloj/notespace "3-beta3"]
                             [aerial.hanami "0.12.4"]
                             [midje/midje "1.9.10"
                               :exclusions [org.clojure/clojure]]]
                   :plugins [[lein-cljfmt "0.7.0"]
                             [jonase/eastwood "0.3.14"]
                             [lein-midje "3.2.1"]]
                   :aliases {"lint" ["do"
                                     ["cljfmt" "check"]
                                     ["eastwood" "{:source-paths [\"src\"]}"]]}}}))
