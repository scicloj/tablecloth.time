(ns tablecloth.time.api
  (:require [tablecloth.api :as tc]
            [tablecloth.time.column.api :as time-col]
            [tablecloth.time.time-literals :refer [modify-printing-of-time-literals-if-enabled!]]))

(modify-printing-of-time-literals-if-enabled!)

;; NOTE: The following legacy APIs have been removed and their tests archived:
;; - tablecloth.time.api.slice (slice, index-by - to be reimplemented)
;; - tablecloth.time.api.adjust-frequency (adjust-frequency - to be reimplemented)
;; - tablecloth.time.api.rolling-window (rolling-window - to be reimplemented)
;; - tablecloth.time.api.converters (various converters - functionality moved to column.api)
;; - tablecloth.time.api.time-components (field extractors - functionality moved to column.api)
;; See test/_archive/README.md for archived tests and development-plan.md for the new architecture.

;; -----------------------------------------------------------------------------
;; Dataset-level time operations
;; -----------------------------------------------------------------------------

(def ^:private field->extractor
  {:year         time-col/year
   :month        time-col/month
   :day          time-col/day
   :hour         time-col/hour
   :minute       time-col/minute
   :second       time-col/get-second
   :day-of-week  time-col/day-of-week
   :day-of-year  time-col/day-of-year
   :week-of-year time-col/week-of-year
   :quarter      time-col/quarter})

(defn add-time-columns
  "Add columns extracted from a datetime column to a dataset.

  time-col — keyword or string name of the source datetime column
  fields   — vector of field keywords, or map of {field target-col-name}

  Supported fields:
    :year, :month, :day, :hour, :minute, :second,
    :day-of-week, :day-of-year, :week-of-year, :quarter

  Examples:
    ;; vector form — column names match field names
    (add-time-columns ds :Month [:year :month])
    ;; => adds :year and :month columns

    ;; map form — explicit output names
    (add-time-columns ds :Month {:year :Year, :month :MonthNum})
    ;; => adds :Year and :MonthNum columns"
  [ds time-col fields]
  (let [field->col (if (map? fields)
                     fields
                     (zipmap fields fields))]
    (reduce-kv (fn [ds field col-name]
                 (if-let [extractor (field->extractor field)]
                   (tc/add-column ds col-name (extractor (ds time-col)))
                   (throw (ex-info (str "Unknown time field: " field
                                        ". Supported: " (keys field->extractor))
                                   {:field field}))))
               ds
               field->col)))
