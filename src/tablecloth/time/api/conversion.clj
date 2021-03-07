(ns tablecloth.time.api.conversion
  (:import [java.time Year YearMonth]
           [org.threeten.extra YearWeek YearQuarter])
  (:require [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype :as dt]
            [tech.v3.datatype.casting :refer [add-object-datatype!]]
            [tick.alpha.api :as tick]
            ))

(set! *warn-on-reflection* true)


(defn year->local-date [^Year year]
  (-> year (.atMonthDay (java.time.MonthDay/parse "--01-01"))))


(defn year->milliseconds-since-epoch [^Year year]
  (-> year year->local-date dtdt/local-date->milliseconds-since-epoch))


(defn milliseconds-since-epoch->year
  ([millis]
   (milliseconds-since-epoch->year millis (dtdt/utc-zone-id)))
  ([millis timezone]
   (-> (dtdt/milliseconds-since-epoch->local-date-time millis timezone)
       (.getYear))))


(defn anytime->milliseconds
  "Converts any time unit type to milliseconds."
  ([str-or-datetime]
   (anytime->milliseconds str-or-datetime (dtdt/utc-zone-id)))
  ([str-or-datetime timezone]
   (let [datetime (if (string? str-or-datetime)
                    (tick/parse str-or-datetime)
                    str-or-datetime)
         datetime-type (tech.v3.datatype/elemwise-datatype datetime)]
     (case datetime-type
       :year
       (year->milliseconds-since-epoch datetime)
       ;; default
       (dtdt/datetime->milliseconds datetime)))))


;; TODO Add information about time unit types to docstring
(defn milliseconds->anytime
  "Convert milliseconds to any time unit as specified by `datetime-type`."
  [millis datetime-type]
  (case datetime-type
    :year
    (milliseconds-since-epoch->year millis)
    ;; default - for cases not specified explicilty above
    ;;           tech.datatype.datetime offers support
    (dtdt/milliseconds->datetime datetime-type millis)))


;; Make tech.v3.datatype.datetime aware of additional java.time classes.
(add-object-datatype! :year java.time.Year true)
(add-object-datatype! :year-month java.time.YearMonth true)


(defn ->instant
  "Convert any datetime to an instant."
  [datetime]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :instant)))


(defmulti convert-to
  "Convert `datetime` to the target time unit as epcified by `unit`"
  (fn [_ unit] unit))

(defmethod convert-to :milliseconds
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/MILLIS)))

(defmethod convert-to :seconds
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/SECONDS)))

(defmethod convert-to :minutes
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/MINUTES)))

(defmethod convert-to :hours
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/HOURS)))

(defmethod convert-to :days
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date-time)
      (.toLocalDate)))

;; TODO: End of week is not the same in all locales
(defmethod convert-to :weeks
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date-time)
      YearWeek/from
      (.atDay java.time.DayOfWeek/SUNDAY)))

(defmethod convert-to :months
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date)
      (.with (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))

(defmethod convert-to :quarters
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date)
      YearQuarter/from
      .atEndOfQuarter))

(defmethod convert-to :years
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date)
      Year/from))
