(defproject tablecloth.time "0.1.0-SNAPSHOT"
  :description "Time series manipulation library built on top of tablecloth"
  :url "http://example.com/FIXME"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojure "1.10.2"]
                 [scicloj/tablecloth "6.00-beta-10"]
                 [tick "0.4.27-alpha"]
                 [org.threeten/threeten-extra "1.5.0"]]

  :profiles
  {:dev {:dependencies [[scicloj/notespace "3-beta3"]
                        [aerial.hanami "0.12.4"]
                        [clj-kondo "2021.03.03"]
                        [midje/midje "1.9.10"
                          :exclusions [org.clojure/clojure]]]
         :plugins [[lein-cljfmt "0.7.0"]
                   [lein-midje "3.2.1"]]
         :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]
                   "lint" ["do"
                           ["cljfmt" "check"]
                           ["run" "-m" "clj-kondo.main" "--lint" "src:test"]]}}})

