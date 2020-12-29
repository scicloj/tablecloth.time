(ns tablecloth.time.scratch
  (:import java.time.LocalDate)
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :refer [index-by slice make-index] :as idx]
            [tick.alpha.api :as t]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [clojure.pprint :refer [pprint]]))

(def path "./data/air-passengers.csv")

(def data
  (-> path
      (tablecloth/dataset
       {:key-fn keyword
        :parser-fn {"Month" [:packed-local-date
                             (fn [date-str]
                               (java.time.LocalDate/parse
                                (str date-str "-01")))]}})
      (tablecloth/rename-columns {:Month :date
                                  :#Passengers :passengers})))

(meta data)

(meta (index-by data :date))

(-> data
    (index-by :date)
    (slice "1949-01-01" "1949-07-01"))

(java.time.LocalDate/parse "1950")
(java.time.Year/parse "1950")

(-> data
    (index-by :date)
    (idx/slice-year "1949" "1955"))

(t/year (t/date "2019-10-10"))
(t/< (t/year "2019") (t/date "2020-01-01") ) 
(t/parse "2019")

(-> (t/year "2019") (.atMonthDay))
(.atMonthDay (t/year "2019") (java.time.MonthDay/parse "--12-03"))

