(ns tablecloth.time.api.converters
  (:import [java.time Year Month LocalDateTime]
           [org.threeten.extra YearWeek YearQuarter])
  (:require [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype :as dt]
            [tablecloth.time.protocols.parseable :as parseable-proto]
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
     ;; default - for cases not specified explicilty above
     ;;           tech.datatype.datetime offers support
     (dtdt/milliseconds->datetime datetime-type timezone millis))))

(defn convert-to
  "Convert time to different type as specified by `datetime-type`."
  [datetime datetime-type]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime datetime-type)))

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

(defn- extract-datetime-component-simple
  [extract-fn datetime]
  (-> datetime
      (->local-date-time)
      (extract-fn)))

;; Would be nice to type hint `time-component`. May need a macro for this.
(defn- extract-datetime-component-complex
  ([extract-fn datetime]
   (extract-datetime-component-complex
    extract-fn
    datetime
    {:as-number? false :as-class? false}))
  ([extract-fn datetime {:keys [as-number? as-class?]}]
   (let [^LocalDateTime ldt (->local-date-time datetime)
         time-component (extract-fn ldt)]
     (cond
       as-number? (.getValue time-component) ;; reflection warning
       as-class? time-component
       :else (.toString ^java.lang.Object time-component)))))

(def ^{:doc "Extracts year (as number) from any datetime."
       :argslist '([datetime])}
  year (partial extract-datetime-component-simple
                #(.getYear ^LocalDateTime %)))

(def ^{:doc "Extracts year (as number) from any datetime."
       :argslist '([datetime])}
  dayofyear (partial extract-datetime-component-simple
                     #(.getYear ^LocalDateTime %)))

(def ^{:doc "Extract month from any datetime."
       :argslist '([datetime] [datetime {:keys [as-number? as-class?]}])}
  month (partial
         extract-datetime-component-complex
         #(.getMonth ^LocalDateTime %)))

(def ^{:doc "Extracts the day of the month from any datetime."
       :argslist '([datetime])}
  dayofmonth (partial extract-datetime-component-simple
                      #(.getDayOfMonth ^LocalDateTime %)))

(def ^{:doc "Extract the day of week from any datetime."
       :argslist '([datetime] [datetime {:keys [as-number? as-class?]}])}
  dayofweek (partial
             extract-datetime-component-complex
             #(.getDayOfWeek ^LocalDateTime %)))

(def ^{:doc "Extracts the hour from any datetime." 
       :arglists '([datetime])}
  hour (partial extract-datetime-component-simple
                  #(.getHour ^LocalDateTime %)))

(def ^{:doc "Extracts the minute of hour (number) from any datetime."
       :arglists '([datetime])}
  minute (partial extract-datetime-component-simple
                  #(.getMinute ^LocalDateTime %)))

(def ^{:doc "Extracts the second of minute from any datetime."
       :arglists '([datetime])}
  seconds (partial extract-datetime-component-simple
                  #(.getSecond ^LocalDateTime %)))
