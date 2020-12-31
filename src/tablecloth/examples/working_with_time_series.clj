(ns tablecloth.examples.working-with-time-series
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as dtype-dt]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as idx]
            [tick.alpha.api :as t]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]))

^kind/md-nocode
["## Dates and Times in Clojure"]

^kind/md-nocode
["Manually build a datetime using the `tick` library."]

(t/new-time 23 59 59 999999)

^kind/md-nocode
["Or, parse from a string."]

(t/parse "2010-10-10")

^kind/md-nocode
["Tick doesn't seem to handle all string formats. In that case, we can use java."]

(java.time.LocalDate/parse
 "Tue, 3 Jun 2008 11:05:30 GMT"
 java.time.format.DateTimeFormatter/RFC_1123_DATE_TIME)

^kind/md-nocode
["If you have a datetime, you can do other things with it, like discover the day of the week."]

(-> (t/parse "2010-10-10")
    (t/day-of-week))

^kind/md-nocode
["## Typed array of times??"]

(def data (dtype/make-container :local-date (list (tech.v3.datatype.datetime/local-date))))
