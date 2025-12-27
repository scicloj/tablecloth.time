(ns tablecloth.time.column.api
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.packing :as dt-packing]
            [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype.datetime.base :as dtdt-base]
            [tech.v3.datatype.datetime.operations :as dtdt-ops]
            [tablecloth.column.api :as tcc])
  (:import [java.time ZoneId Instant ZonedDateTime LocalDate LocalDateTime
            Duration LocalTime]))

(casting/add-object-datatype! :instant Instant true)
(casting/add-object-datatype! :zoned-date-time ZonedDateTime true)
(casting/add-object-datatype! :local-date LocalDate true)
(casting/add-object-datatype! :local-date-time LocalDateTime true)
(casting/add-object-datatype! :duration Duration true)
(casting/add-object-datatype! :local-time LocalTime true)

(def ^:private targets
  (apply conj dtdt-base/datatypes
         dtdt-base/epoch-datatypes))

(def ^:private synonyms
  {:zdt :zoned-date-time
   :odt :offset-date-time
   :ldt :local-date-time})

(defn calendar-local-type? [dtype]
  (let [calendar-local-types #{:local-date :local-date-time :local-time}
        base-type (dt-packing/unpack-datatype dtype)]
    (boolean (calendar-local-types base-type))))

(defn ^:private normalize-target
  "Normalize a target designator (keyword or Class) to a canonical keyword in `targets`.

  TODO: Right now dtype-next's object-class->datatype return :object for unsupported types
  bypassing our error messaging. Need to decide if this is good."
  [t]
  (cond
    (keyword? t) (let [t' (get synonyms t t)]
                   (if (contains? targets t')
                     t'
                     (throw (ex-info (str "Unsupported target: " t)
                                     {:type ::unsupported-target :target t}))))
    (class? t) (if-some [kw (casting/object-class->datatype t)]
                 kw
                 (throw (ex-info (str "Unsupported target class: " t)
                                 {:type ::unsupported-target-class :target t})))
    :else (throw (ex-info (str "Target must be a keyword or Class: " (pr-str t))
                          {:type ::invalid-target :target t}))))

(defn ^:private coerce-zone-id
  "Coerce nil/String/ZoneId to a ZoneId.

  - 1-arg arity: nil -> system zone.
  - 2-arg arity: nil -> (:default opts)."
  (^ZoneId [z]
   (coerce-zone-id z {:default (dtdt/system-zone-id)}))
  ([z opts]
   (cond
     (nil? z) (:default opts)
     (instance? ZoneId z) ^ZoneId z

     (string? z) (ZoneId/of ^String z)
     :else (throw (ex-info (str "Unsupported zone value: " (pr-str z))
                           {:type ::unsupported-zone
                            :value z})))))

(defn ^:private coerce-column
  "Coerce to a column."
  [data]
  (if (tcc/column? data)
    data
    (tcc/column data)))

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

(defn ^:private metric-unit?
  "Check if unit is a metric/fixed-duration unit."
  [u]
  (boolean (#{:milliseconds :seconds :minutes :hours :days :weeks}
            (normalize-unit u))))

(defn ^:private calendar-unit?
  "Check if unit is a calendar/variable-duration unit."
  [u]
  (boolean (#{:months :quarters :years} (normalize-unit u))))

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
         zone (coerce-zone-id (:zone opts)
                              {:default (dtdt/utc-zone-id)})
         src-type (dtype/elemwise-datatype col)
         src-cat (dtdt-base/classify-datatype src-type)
         tgt-type (normalize-target target)
         tgt-cat (dtdt-base/classify-datatype tgt-type)
         data
         (case [src-cat tgt-cat]
           [:epoch :temporal]
           (if (calendar-local-type? tgt-type)
             ;; zone only needed when temporal is calendar local.
             (dtdt/epoch->datetime zone src-type tgt-type col)
             (dtdt/epoch->datetime tgt-type col))
           [:temporal :epoch]
           (if (calendar-local-type? src-type)
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
         original-type (dt-packing/unpack-datatype (dtype/elemwise-datatype col))
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
         original-type (dt-packing/unpack-datatype (dtype/elemwise-datatype col))
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
         original-type (dt-packing/unpack-datatype (dtype/elemwise-datatype col))
         col-ld (convert-time col :local-date opts)
         col-epoch-quarter (local-date->epoch-quarter col-ld)
         col-floored (fun/- col-epoch-quarter (fun/rem col-epoch-quarter interval))
         ;; Convert quarters back to months, then add to epoch
         col-floored-months (fun/* col-floored 3)
         result-ld (dtdt-ops/plus-temporal-amount (LocalDate/of 1970 1 1) col-floored-months :months)]
     (convert-time result-ld original-type opts))))

(defn down-to-nearest
  ""
  ([col interval unit opts]
   (let [col (coerce-column col)
         original-type (dt-packing/unpack-datatype (dtype/elemwise-datatype col))
         unit (normalize-unit unit)
         zone (coerce-zone-id (:zone opts))]
     (cond
       (metric-unit? unit)
       (let [divisor (* (long interval) (long (milliseconds-in unit)))
             millis-col (convert-time col :epoch-milliseconds {:zone zone})
             rounded-col (dtype/set-datatype
                          ;; must set type b/c arithmetic comes back
                          ;; as :int64 -- the storage type of
                          ;; epoch-milliseconds
                          (fun/- millis-col (fun/rem millis-col divisor))
                          :epoch-milliseconds)]
         (convert-time rounded-col original-type (:zone zone)))
       (calendar-unit? unit)
       (let [column-zdt (convert-time col :zoned-date-time {:zone zone})
             rounded-col (case unit
                           :months (floor-to-month interval opts))]
         (convert-time rounded-col original-type))))))

