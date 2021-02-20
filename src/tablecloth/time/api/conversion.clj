(ns tablecloth.time.api.conversion
  (:import [java.time Year]
           [org.threeten.extra YearWeek])
  (:require [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype :as dt]
            [tech.v3.datatype.casting :refer [add-object-datatype!]]
            [tick.alpha.api :as tick]))

(set! *warn-on-reflection* true)

(defn- parse-anytime [str-or-datetime]
  (if (string? str-or-datetime)
    (tick/parse str-or-datetime)
    str-or-datetime))

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

(defn milliseconds->anytime [millis datetime-type]
  (case datetime-type
    :year
    (milliseconds-since-epoch->year millis)
    (dtdt/milliseconds->datetime datetime-type millis)))

;; Make tech.v3.datatype.datetime aware of additional java.time classes.
(add-object-datatype! :year java.time.Year true)
(add-object-datatype! :year-month java.time.YearMonth true)

(defn ->instant [datetime]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :instant)))

(defmulti truncate-to (fn [_ unit] unit))

(defmethod truncate-to :milliseconds
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/MILLIS)))

(defmethod truncate-to :seconds
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/SECONDS)))

(defmethod truncate-to :minutes
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/MINUTES)))

(defmethod truncate-to :hours
  [datetime _]
  (-> datetime
      ->instant
      (.truncatedTo java.time.temporal.ChronoUnit/HOURS)))

(defmethod truncate-to :days
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date-time)
      (.toLocalDate)))

;;    :week (fn [datetime] (-> datetime
;;                             (YearWeek/from)
;;                             (.atDay java.time.DayOfWeek/SUNDAY)))
(defmethod truncate-to :week
  [datetime _]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date-time)
      ))

(comment
  (dt/e
  (anytime->milliseconds #time/year "2015")
  (anytime->milliseconds (dtdt/instant))


  (truncate-to (tick/year) :minutes)

  (-> #time/date "2020-10-08"
      (anytime->milliseconds)
      (milliseconds->anytime :local-date)
      YearWeek/from
      (.atDay java.time.DayOfWeek/SUNDAY)
      )

  (java.time.temporal.WeekFields/of (dtdt/locale))

  ;; week
  (-> #time/instant "2021-02-15T07:27:38.676Z"
      anytime->milliseconds
      (quot dtdt/milliseconds-in-week)
      (* dtdt/milliseconds-in-week)
      (+ dtdt/milliseconds-in-week)
      (milliseconds->anytime :instant)
      )
)
