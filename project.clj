(defproject org.scicloj/tablecloth.time "1.00-alpha-4-SNAPSHOT"
  :description "A time series manipulation library built on top of tablecloth."
  :url "https://github.com/scicloj/tablecloth.time"
  :license {:name "The MIT Licence"
            :url "https://opensource.org/licenses/MIT"}

  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]

  :lein-tools-deps/config {:config-files [:install :user :project]}

  :profiles {:dev {:lein-tools-deps/config {:aliases [:dev]}
                   :aliases {"clj-kondo" ["run" "-m" "clj-kondo.main"]
                             "lint" ["do"
                                     ["cljfmt" "check"]
                                     ["run" "-m" "clj-kondo.main" "--lint" "src:test"]]}
                   :plugins [[lein-midje "3.2.1"]
                             [lein-cljfmt "0.7.0"]]}})

