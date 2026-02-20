(ns tablecloth.time.column.api
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.base :as dtype-base]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype.datetime.base :as dtdt-base]
            [tech.v3.datatype.datetime.operations :as dtdt-ops]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.utils.datatypes :as datatypes]
            [tablecloth.time.utils.temporal :as temporal]
            [tablecloth.time.utils.units :as units]
            [tablecloth.api :as tc])
  (:import [java.time Instant ZonedDateTime LocalDate LocalDateTime
            Duration LocalTime]))

(casting/add-object-datatype! :instant Instant true)
(casting/add-object-datatype! :zoned-date-time ZonedDateTime true)
(casting/add-object-datatype! :local-date LocalDate true)
(casting/add-object-datatype! :local-date-time LocalDateTime true)
(casting/add-object-datatype! :duration Duration true)
(casting/add-object-datatype! :local-time LocalTime true)

(defn coerce-column
  "Coerce data to a column."
  [data]
  (if (tcc/column? data)
    data
    (tcc/column data)))

(defn convert-time
  "Convert a time column between temporal and epoch representations.

  opts map:
  - :zone — ZoneId or zone string used when a temporal representation needs
            a zone (e.g. LocalDate/LocalDateTime/LocalTime or when producing
            zoned types). Defaults to UTC when not provided.
  "
  ([col target]
   (convert-time col target nil))
  ([col target opts]
   (let [col (coerce-column col)
         zone (temporal/coerce-zone-id (:zone opts)
                                       {:default (dtdt/utc-zone-id)})
         src-type (dtype/elemwise-datatype col)
         src-cat (dtdt-base/classify-datatype src-type)
         tgt-type (temporal/normalize-target target)
         tgt-cat (dtdt-base/classify-datatype tgt-type)
         data
         (case [src-cat tgt-cat]
           [:epoch :temporal]
           (if (temporal/calendar-local-type? tgt-type)
             ;; zone only needed when temporal is calendar local.
             (dtdt/epoch->datetime zone src-type tgt-type col)
             (dtdt/epoch->datetime tgt-type col))
           [:temporal :epoch]
           (if (temporal/calendar-local-type? src-type)
             ;; zone only needed when temporal is calendar local.
             (dtdt/datetime->epoch zone tgt-type col)
             (dtdt/datetime->epoch tgt-type col))
           [:temporal :temporal]
           (->> col
                (dtdt/datetime->milliseconds zone)
                (dtdt/milliseconds->datetime tgt-type zone))
           [:epoch :epoch]
           (if (= src-type tgt-type)
             col
             (let [src-factor (dtdt-base/epoch->microseconds src-type)
                   tgt-factor (dtdt-base/epoch->microseconds tgt-type)
                   conversion (fun// src-factor tgt-factor)]
               (fun/* col conversion)))
           (throw
            (ex-info
             (format
              (str "Unsupported time conversion from %s (%s) to %s (%s). "
                   "`convert-time` only supports conversions among temporal and "
                   "epoch types; duration/relative types require dedicated APIs.")
              src-type src-cat tgt-type tgt-cat)
             {:type          ::unsupported-time-conversion
              :col-type      (type col)
              :src-type      src-type
              :src-category  src-cat
              :tgt-type      tgt-type
              :tgt-category  tgt-cat})))]
     (tcc/column data))))

(defn- local-date->epoch-month [col-ld]
  (let [col-year (dtdt-ops/long-temporal-field :years col-ld)
        col-year-month (dtdt-ops/long-temporal-field :months col-ld)
        col-epoch-month (fun/+ (fun/* 12 (fun/- col-year 1970))
                               (fun/- col-year-month 1))]
    col-epoch-month))

(defn- local-date->epoch-year [col-ld]
  (let [col-year (dtdt-ops/long-temporal-field :years col-ld)
        col-epoch-year (fun/- col-year 1970)]
    col-epoch-year))

(defn floor-to-year
  "Floor temporal column values to the closest year interval."
  ([col interval]
   (floor-to-year col interval {:zone (dtdt/system-zone-id)}))
  ([col interval opts]
   (let [col (coerce-column col)
         original-type (datatypes/get-datatype col)
         col-ld (convert-time col :local-date opts)
         col-epoch-year (local-date->epoch-year col-ld)
         col-floored (fun/- col-epoch-year (fun/rem col-epoch-year interval))
         result-ld (dtdt-ops/plus-temporal-amount (LocalDate/of 1970 1 1) col-floored :years)]
     (convert-time result-ld original-type opts))))

(defn floor-to-month
  "Floor a temporal columns values the closest month interval."
  ([col interval]
   (floor-to-month col interval {:zone (dtdt/system-zone-id)}))
  ([col interval opts]
   (let [col (coerce-column col)
         original-type (datatypes/get-datatype col)
         col-ld (convert-time col :local-date opts)
         col-epoch-month (local-date->epoch-month col-ld)
         col-floored (fun/- col-epoch-month (fun/rem col-epoch-month interval))
         result-ld (dtdt-ops/plus-temporal-amount (LocalDate/of 1970 1 1) col-floored :months)]
     (convert-time result-ld original-type opts))))

(defn- local-date->epoch-quarter
  "Convert LocalDate column to epoch-quarters (quarters since 1970-Q1)."
  [col-ld]
  (let [col-epoch-month (local-date->epoch-month col-ld)
        col-epoch-quarter (fun/quot col-epoch-month 3)]
    col-epoch-quarter))

(defn floor-to-quarter
  "Floor temporal column values to the closest quarter interval."
  ([col interval]
   (floor-to-quarter col interval {:zone (dtdt/system-zone-id)}))
  ([col interval opts]
   (let [col (coerce-column col)
         original-type (datatypes/get-datatype col)
         col-ld (convert-time col :local-date opts)
         col-epoch-quarter (local-date->epoch-quarter col-ld)
         col-floored (fun/- col-epoch-quarter (fun/rem col-epoch-quarter interval))
         ;; Convert quarters back to months, then add to epoch
         col-floored-months (fun/* col-floored 3)
         result-ld (dtdt-ops/plus-temporal-amount (LocalDate/of 1970 1 1) col-floored-months :months)]
     (convert-time result-ld original-type opts))))

(defn down-to-nearest
  "Floor a `col` of time values to the nearest lower multiple of (interval × unit).

    Arities:
    - (down-to-nearest interval unit) => returns a function f; (f col) or (f x col)
    - (down-to-nearest col interval unit)
    - (down-to-nearest col interval unit opts)

    Semantics:
    - Converts x to epoch millis, floors by modulo (toward −∞), then converts back,
      preserving x's type (Instant/LocalDateTime/LocalDate/ZonedDateTime/OffsetDateTime).
    - If x is a number (millis), returns a number.
    - Units: :milliseconds :seconds :minutes :hours :days :weeks, and :months/:quarters/:years (calendar-aware).
    - LocalDate/LocalDateTime use the system default zone by default; pass opts with :zone to override."
  ([col interval unit opts]
   (let [col (coerce-column col)
         original-type (datatypes/get-datatype col)
         unit (units/normalize-unit unit)
         zone (temporal/coerce-zone-id (:zone opts))]
     (cond
       (units/metric-unit? unit)
       (let [divisor (* (long interval) (long (units/milliseconds-in unit)))
             millis-col (convert-time col :epoch-milliseconds {:zone zone})
             rounded-col (dtype/set-datatype
                          ;; must set type b/c arithmetic comes back
                          ;; as :int64 -- the storage type of
                          ;; epoch-milliseconds
                          (fun/- millis-col (fun/rem millis-col divisor))
                          :epoch-milliseconds)]
         (convert-time rounded-col original-type (:zone zone)))
       (units/calendar-unit? unit)
       (let [col (convert-time col :local-date)
             rounded-col (case unit
                           :months (floor-to-month col interval opts)
                           :years  (floor-to-year col interval opts)
                           :quarters (floor-to-quarter col interval opts))]
         (convert-time rounded-col original-type))))))

;; -----------------------------------------------------------------------------
;; Field extractors
;; -----------------------------------------------------------------------------

(defn- ensure-local-datetime
  "Convert Instant columns to LocalDateTime (UTC) for field extraction.
  Other datetime types are left as-is since they already have calendar context."
  [col]
  (let [col (coerce-column col)
        base-dtype (datatypes/get-datatype col)]
    (if (= base-dtype :instant)
      (convert-time col :local-date-time {:zone (dtdt/utc-zone-id)})
      col)))

(defn year
  "Extract year from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :years (ensure-local-datetime col))))

(defn month
  "Extract month (1-12) from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :months (ensure-local-datetime col))))

(defn day
  "Extract day of month from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :days (ensure-local-datetime col))))

(defn hour
  "Extract hour from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :hours (ensure-local-datetime col))))

(defn minute
  "Extract minute from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :minutes (ensure-local-datetime col))))

(defn get-second
  "Extract second from a datetime column.
  
  Named `get-second` to avoid collision with clojure.core/second."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :seconds (ensure-local-datetime col))))

(defn day-of-week
  "Extract day of week from a datetime column. Monday=1, Sunday=7 (ISO standard)."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :iso-day-of-week (ensure-local-datetime col))))

(defn day-of-year
  "Extract day of year (1-366) from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :day-of-year (ensure-local-datetime col))))

(defn week-of-year
  "Extract ISO week of year (1-53) from a datetime column."
  [col]
  (tcc/column (dtdt-ops/long-temporal-field :iso-week-of-year (ensure-local-datetime col))))

(defn quarter
  "Extract quarter (1-4) from a datetime column."
  [col]
  (let [months (dtdt-ops/long-temporal-field :months (ensure-local-datetime col))]
    (tcc/column (fun/+ 1 (fun/quot (fun/- months 1) 3)))))

;; -----------------------------------------------------------------------------
;; Lag and Lead operations
;; -----------------------------------------------------------------------------

(defn lag
  "Shift values forward by k positions, filling the first k positions with nil.

  Returns a new column of the same type with nil at the start and the
  original values shifted forward. The nil values are properly tracked
  by tech.ml.dataset's missing value system.

  Example:
    (lag [A B C D E] 2)  ;; => [nil nil A B C]
    (lag col 4)          ;; => [nil nil nil nil ...first n-4 values...]"
  [col k]
  (let [col (coerce-column col)
        n (dtype/ecount col)
        values (dtype-base/select col (range 0 (- n k)))]
    ;; Build new column: k nils + values
    ;; tcc/column handles nil properly in numeric columns via TMD's missing value support
    (tcc/column (concat (repeat k nil) values))))

(defn lead
  "Shift values backward by k positions, filling the last k positions with nil.

  Returns a new column of the same type with nil at the end and the
  original values shifted backward.

  Example:
    (lead [A B C D E] 2)  ;; => [C D E nil nil]
    (lead col 4)          ;; => [...values from index 4 onward... nil nil nil nil]"
  [col k]
  (let [col (coerce-column col)
        n (dtype/ecount col)
        values (dtype-base/select col (range k n))]
    ;; Build new column: values + k nils
    (tcc/column (concat values (repeat k nil)))))

