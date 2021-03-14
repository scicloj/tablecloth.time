(ns tablecloth.time.api.conversion
  (:import [java.time Year YearMonth]
           [org.threeten.extra YearWeek YearQuarter])
  (:require [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype :as dt]
            [tech.v3.datatype.casting :refer [add-object-datatype!]]
            [tick.alpha.api :as tick]))

(set! *warn-on-reflection* true)

;; TODO add more support for conversion targets
;; - nanoseconds
;; - microseconds
;; - business day
;; - business month end
;; - busness quarter end
;; - business year end
;; - business hours

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

(defn ->local-date-time
  "Convert any datetime to a local datetime."
  [datetime]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date-time)))

(defn ->local-date
  "Convert any datetime to a local date."
  [datetime]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime :local-date)))

(defn milliseconds-in [chrono-unit]
  (case chrono-unit
    :seconds
    dtdt/milliseconds-in-second
    :minutes
    dtdt/milliseconds-in-minute
    :hours
    dtdt/milliseconds-in-hour))

(defn round-down-to-nearest
  ([interval chrono-unit]
   (partial round-down-to-nearest interval chrono-unit))
  ([interval chrono-unit datetime]
   (let [millis  (anytime->milliseconds datetime)
         divisor (* interval (milliseconds-in chrono-unit))]
     (milliseconds->anytime (- millis (mod millis divisor))))))

;; (defn ->every
;;   ([interval chrono-unit]
;;    (partial ->every interval chrono-unit))
;;   ([interval chrono-unit-kw datetime]
;;    (let [ms-in-chrono-unit (milliseconds-in chrono-unit-kw)
;;          datetime-in-ms (-> datetime anytime->milliseconds)
;;          divisor (* interval ms-in)
;;          remainder (mod ms divisor)
;;          new-ms (- ms remainder)]
;;      ()
;;      (milliseconds->anytime new-ms :instant))))


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

;; (defn ->weeks
;;   [datetime]
;;   ->local-date-time
;;   YearWeek/from
;;   (.atDay java.time.DayOfWeek/SUNDAY))

(defn ->months
  [datetime]
  (let [^java.time.LocalDate local-date (->local-date datetime)]
    (.with local-date (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))

(defn ->quarters
  [datetime]
  (-> datetime
      ->local-date-time
      YearQuarter/from
      .atEndOfQuarter))

(defn ->years
  [datetime]
  (-> datetime ->local-date Year/from))


(comment
  (milliseconds-in :seconds)


  (->every 5 :seconds)

  (def ->my-second (->every 1 :seconds))


  (->seconds #time/date "1970-01-01")
;; => #time/instant "1970-01-01T00:00:00Z"
  (->my-second #time/date "1970-01-01")
;; => #time/instant "1970-01-01T00:00:00Z"

  (defn compute-with-time-measurement [f]
    (let [start-time (dtdt/instant)
          result (f)
          end-time (dtdt/instant)]
      {:result result
      :duration (dtdt/between start-time end-time :milliseconds)}))

  )


