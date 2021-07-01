(ns tablecloth.time.utils.year-quarter-helpers
  (:import [org.threeten.extra YearQuarter])
  (:require [tech.v3.datatype.datetime :as dtdt]))

(defn year-quarter->local-date [^YearQuarter year-quarter]
  (.atDay year-quarter 1))

(defn year-quarter->milliseconds-since-epoch [^YearQuarter year-quarter]
  (-> year-quarter year-quarter->local-date dtdt/local-date->milliseconds-since-epoch))

(defn milliseconds-since-epoch->year-quarter
  ([millis]
   (milliseconds-since-epoch->year-quarter millis (dtdt/utc-zone-id)))
  ([millis timezone]
   (let [local-date (dtdt/milliseconds-since-epoch->local-date-time millis timezone)
         year       (.getYear local-date)
         quarter    (.get local-date java.time.temporal.IsoFields/QUARTER_OF_YEAR)]
     (YearQuarter/of year quarter))))
