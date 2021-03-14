(defproject tablecloth.time "0.1.0-SNAPSHOT"
  :description "Time series manipulation library built on top of tablecloth"
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.2"]
                 [scicloj/tablecloth "5.04"]
                 [tick "0.4.27-alpha"]
                 [scicloj/notespace "3-alpha3-SNAPSHOT"]
                 [aerial.hanami "0.12.4"]]
  :profiles {:dev {:cloverage {:runner :midje}
                   :dependencies [[midje "1.9.10"]]
                   :plugins [[lein-midje "3.2.1"]
                             [lein-cloverage "1.1.2"]]}})
