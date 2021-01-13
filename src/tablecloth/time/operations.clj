(ns tablecloth.time.operations
  (:import java.util.TreeMap)
  (:require [tablecloth.time.index :refer [get-index-meta]]
            [tablecloth.api :as tablecloth]
            [tech.v3.datatype.errors :as errors]
            [tick.alpha.api :as t]))

(set! *warn-on-reflection* true)

;; This method treats the from/to keys naively. When we attempt
;; to unify the API into a single slice method this may go away.
(defn get-slice [dataset from to]
  (let [^TreeMap index (get-index-meta dataset)
        row-numbers (if (not index)
                      (throw (Exception. "Dataset has no index specified."))
                      (-> index (.subMap from true to true) (.values)))]
    (tablecloth/select-rows dataset row-numbers)))

;; TODO Write single `slice` method to handle all time units

(defn slice-by-year [dataset ^String from  ^String to]
  (let [from-year (t/year from)
        to-year (t/year to)]
    (get-slice dataset from-year to-year)))

(defn slice-by-date [dataset from to]
  (let [from-date (t/date from)
        to-date (t/date to)]
    (get-slice dataset from-date to-date)))

(defn slice-by-datetime [dataset from to]
  (let [from-datetime (t/date-time from)
        to-datetime (t/date-time to)]
    (get-slice dataset from-datetime to-datetime)))
