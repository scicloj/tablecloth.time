(ns tablecloth.time.utils.year-month-helpers
  (:import [java.time YearMonth])
  (:require [tech.v3.datatype.datetime :as dtdt]))

(defn year-month->local-date [^YearMonth year-month]
  (.atDay year-month 1))

(defn year-month->milliseconds-since-epoch [^YearMonth year-month]
  (-> year-month year-month->local-date dtdt/local-date->milliseconds-since-epoch))

(defn milliseconds-since-epoch->year-month
  ([millis]
   (milliseconds-since-epoch->year-month millis (dtdt/utc-zone-id)))
  ([millis timezone]
   (let [local-date (dtdt/milliseconds-since-epoch->local-date-time millis timezone)
         year       (.getYear local-date)
         month      (.getMonth local-date)]
     (YearMonth/of year month)))) 
