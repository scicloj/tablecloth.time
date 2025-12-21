(ns tablecloth.time.column.api
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [tech.v3.datatype.packing :as dt-packing]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.datatype.datetime.base :as dtdt-base]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.api.parse :refer [parse]])
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
                   conversion (fun// src-factor tgt-factor )]
               (fun/* col conversion)))
           (throw
            (ex-info
             (format
              (str "Unsupported time conversion from %s (%s) to %s (%s). "
                   "`convert-time` only supports conversions among temporal and "
                   "epoch types; duration/relative types require dedicated APIs.")
              src-type src-cat tgt-type tgt-cat)
             {:type          ::unsupported-time-conversion
              :src-type      src-type
              :src-category  src-cat
              :tgt-type      tgt-type
              :tgt-category  tgt-cat})))]
     (tcc/column data))))

(comment
  ;; source days => target hours

  ;; milliseconds -> days 

  (tcc/column (tcc/column))

  (fun//
   (dt-base/epoch->microseconds :epoch-milliseconds)
   (dt-base/epoch->microseconds :epoch-days)
   )

  (dt-base/epoch->microseconds :epoch-hours)

  (parse "2025-12-10")

  (def ddata (tcc/column (map parse ["1970-01-02" "1970-01-10"])))

  (-> ddata
      (convert-time :epoch-milliseconds))

  (-> ddata
      (convert-time :epoch-milliseconds)
      (convert-time :epoch-days)
      )

)

