(ns tablecloth.time.scratch
  (:import java.time.LocalDate)
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :refer [index-by slice make-index]]
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
