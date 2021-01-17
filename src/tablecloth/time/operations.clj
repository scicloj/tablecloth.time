(ns tablecloth.time.operations
  (:import java.util.TreeMap)
  (:require [tablecloth.time.index :refer [get-index-type slice-index]]
            [tablecloth.api :as tablecloth]
            [tech.v3.datatype.errors :as errors]
            [tick.alpha.api :as t]))

(set! *warn-on-reflection* true)

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
  java.time.LocalDateTime
  [_ date-str]
  (java.time.LocalDateTime/parse date-str))

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
    (slice-index dataset from-key to-key)))


