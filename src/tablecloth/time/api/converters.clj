(ns tablecloth.time.api.converters
  (:import [org.threeten.extra YearWeek YearQuarter])
  (:require [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype :as dt]
            [tablecloth.time.protocols.parseable :as parseable-proto]
            [tablecloth.time.utils.year-helpers :refer [year->local-date
                                                        year->milliseconds-since-epoch
                                                        milliseconds-since-epoch->year]]
            [tablecloth.time.utils.year-month-helpers :refer [year-month->local-date
                                                              year-month->milliseconds-since-epoch
                                                              milliseconds-since-epoch->year-month]]
            [tablecloth.time.utils.year-quarter-helpers :refer [year-quarter->local-date
                                                                year-quarter->milliseconds-since-epoch
                                                                milliseconds-since-epoch->year-quarter]]
            [tech.v3.datatype.casting :refer [add-object-datatype!]]))

(set! *warn-on-reflection* true)

;; TODO add more support for conversion targets
;; - nanoseconds
;; - microseconds
;; - business day
;; - business month end
;; - busness quarter end
;; - business year end
;; - business hours

(defn string->time
  "Given an identifiable time, returns the correct datetime object.
  Optionally, you can specify a target type to also convert to a
  different type in one step.

  TODO: How do we define what an 'identifiable' string means?"
  [str]
  (parseable-proto/parse str))

(defn anytime->milliseconds
  "Converts any time unit type to milliseconds."
  ([str-or-datetime]
   (anytime->milliseconds str-or-datetime (dtdt/utc-zone-id)))
  ([str-or-datetime timezone]
   (let [datetime (if (string? str-or-datetime)
                    (string->time str-or-datetime)
                    str-or-datetime)
         datetime-type (tech.v3.datatype/elemwise-datatype datetime)]
     (case datetime-type
       :year
       (year->milliseconds-since-epoch datetime)
       :year-month
       (year-month->milliseconds-since-epoch datetime)
       :year-quarter
       (year-quarter->milliseconds-since-epoch datetime)
       ;; default
       (dtdt/datetime->milliseconds timezone datetime)))))

(defn milliseconds->anytime
  "Convert milliseconds to any time unit as specified by `datetime-type`."
  ([millis datetime-type]
   (milliseconds->anytime millis datetime-type (dtdt/utc-zone-id)))
  ([millis datetime-type timezone]
   (case datetime-type
     :year
     (milliseconds-since-epoch->year millis timezone)
     :year-month
     (milliseconds-since-epoch->year-month millis timezone)
     :year-quarter
     (milliseconds-since-epoch->year-quarter millis timezone)
     ;; default - for cases not specified explicilty above
     ;;           tech.datatype.datetime offers support
     (dtdt/milliseconds->datetime datetime-type timezone millis))))

(defn convert-to
  "Convert time to different type as specified by `datetime-type`."
  [datetime datetime-type]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime datetime-type)))

(defn ->local-date-time
  "Convert any datetime to a local datetime."
  [datetime]
  (convert-to datetime :local-date-time))

(defn ->zoned-date-time
  "Convert any datetime type to a zoned date time."
  [datetime]
  (convert-to datetime :zoned-date-time))

(defn ->instant
  "Convert any datetime to an instant."
  [datetime]
  (convert-to datetime :instant))

(defn ->local-date
  "Convert any datetime to a local date."
  [datetime]
  (convert-to datetime :local-date))

(defn ->year
  "Convert any datetime type to a year."
  [datetime]
  (convert-to datetime :year))

(defn ->year-month
  "Convert any datetime type to a year month."
  [datetime]
  (convert-to datetime :year-month))

(defn milliseconds-in [chrono-unit]
  (case chrono-unit
    :milliseconds
    1
    :seconds
    dtdt/milliseconds-in-second
    :minutes
    dtdt/milliseconds-in-minute
    :hours
    dtdt/milliseconds-in-hour
    :days
    dtdt/milliseconds-in-day
    :weeks
    dtdt/milliseconds-in-week
    ;;default
    (throw (Exception. (str "Can't determine milliseconds in: " chrono-unit)))))

(defn down-to-nearest
  ([interval chrono-unit]
   (partial down-to-nearest interval chrono-unit))
  ([interval chrono-unit datetime]
   (let [millis  (anytime->milliseconds datetime)
         divisor (* interval (milliseconds-in chrono-unit))
         rounded-millis (- millis (mod millis divisor))
         datetime-type (dt/elemwise-datatype datetime)]
     (milliseconds->anytime rounded-millis datetime-type))))

;; alias for round-down-to-nearest
(defn ->every
  ([interval chrono-unit]
   (partial down-to-nearest interval chrono-unit))
  ([interval chrono-unit datetime]
   (down-to-nearest interval chrono-unit datetime)))

(defn ->seconds
  [datetime]
  (let [^java.time.Instant inst (->instant datetime)]
    (.truncatedTo inst java.time.temporal.ChronoUnit/SECONDS)))

(defn ->minutes
  [datetime]
  (let [^java.time.Instant inst (->instant datetime)]
    (.truncatedTo inst java.time.temporal.ChronoUnit/MINUTES)))

(defn ->hours
  [datetime]
  (let [^java.time.Instant inst (->instant datetime)]
    (.truncatedTo inst java.time.temporal.ChronoUnit/HOURS)))

(defn ->days
  [datetime]
  (-> datetime ->local-date))

(defn ->weeks-end
  [datetime]
  (-> datetime
      ->local-date-time
      YearWeek/from
      (.atDay java.time.DayOfWeek/SUNDAY)))

(defn ->months-end
  [datetime]
  (let [^java.time.LocalDate local-date (->local-date datetime)]
    (.with local-date (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))

(defn ->quarters-end
  [datetime]
  (-> datetime
      ->local-date-time
      YearQuarter/from
      .atEndOfQuarter))

(defn ->years-end
  [datetime]
  (let [^java.time.LocalDate localDate (-> datetime ->local-date)]
    (.with localDate (java.time.temporal.TemporalAdjusters/lastDayOfYear))))
