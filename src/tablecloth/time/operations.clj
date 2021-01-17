(ns tablecloth.time.operations
  (:import java.util.TreeMap)
  (:require [tablecloth.time.index :refer [get-index-meta get-index-type]]
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

(defmulti parse-datetime-str
  (fn [datetime-datatype _] datetime-datatype))

(defmethod parse-datetime-str
  java.time.Instant
  [_ date-str]
  (java.time.Instant/parse date-str))

(defmethod parse-datetime-str
  java.time.ZonedDateTime
  [_ date-str]
  (java.time.ZonedDateTime/parse date-str))

(defmethod parse-datetime-str
  java.time.LocalDate
  [_ date-str]
  (java.time.LocalDate/parse date-str))

(defmethod parse-datetime-str
  java.time.YearMonth
  [_ date-str]
  (java.time.YearMonth/parse date-str))

(defmethod parse-datetime-str
  java.time.Year
  [_ date-str]
  (java.time.Year/parse date-str))

(defn slice [dataset from to]
  (let [time-unit (get-index-type dataset)
        from-key (parse-datetime-str time-unit from)
        to-key (parse-datetime-str time-unit to)]
    (get-slice dataset from-key to-key)))


