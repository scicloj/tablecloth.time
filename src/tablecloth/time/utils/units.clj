(ns tablecloth.time.utils.units
  (:require [tech.v3.datatype.datetime :as dtdt]))

(defn normalize-unit
  "Normalize a unit across plural or singular usages. Plural is the
  normalized version. Noop if type is not supported."
  [u]
  (case u
    :second  :seconds
    :minute  :minutes
    :hour    :hours
    :day     :days
    :week    :weeks
    :month   :months
    :year    :years
    :quarter :quarters
    u))

(defn metric-unit?
  "Check if unit is a metric/fixed-duration unit."
  [u]
  (boolean (#{:milliseconds :seconds :minutes :hours :days :weeks}
            (normalize-unit u))))

(defn calendar-unit?
  "Check if unit is a calendar/variable-duration unit."
  [u]
  (boolean (#{:months :quarters :years} (normalize-unit u))))

(defn milliseconds-in
  "Return the number of epoch millis in a unit.

  Units: :milliseconds :seconds :minutes :hours :days :weeks."
  [unit]
  (case unit
    :milliseconds 1
    :seconds      dtdt/milliseconds-in-second
    :minutes      dtdt/milliseconds-in-minute
    :hours        dtdt/milliseconds-in-hour
    :days         dtdt/milliseconds-in-day
    :weeks        dtdt/milliseconds-in-week
    (throw (ex-info (str "Unsupported unit: " unit) {:unit unit}))))
