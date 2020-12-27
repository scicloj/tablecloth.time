(ns tablecloth.time.scratch
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :refer [index-by slice]]
            [clojure.pprint :refer [pprint]]))

(def path "./data/air-passengers.csv")

(def data
  (-> path
      (tablecloth/dataset
       {:key-fn keyword
        :parser-fn {"Month" [:packed-local-date
                             (fn [date-str]
                               (java.time.LocalDate/parse
                                (str date-str "-01")))]}})))

(meta data)

(-> data
    (index-by :Month)
    (slice "1949-01-01" "1949-07-01"))

