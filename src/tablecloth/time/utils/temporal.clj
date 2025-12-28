(ns tablecloth.time.utils.temporal
  (:require [tech.v3.datatype.packing :as dt-packing]
            [tech.v3.datatype.casting :as casting]
            [tech.v3.datatype.datetime.base :as dtdt-base]
            [tech.v3.datatype.datetime :as dtdt]
            [tablecloth.column.api :as tcc])
  (:import [java.time ZoneId]))

(def targets
  "Set of valid temporal target types for conversion."
  (apply conj dtdt-base/datatypes
         dtdt-base/epoch-datatypes))

(def synonyms
  "Map of shorthand type aliases to canonical type keywords."
  {:zdt :zoned-date-time
   :odt :offset-date-time
   :ldt :local-date-time})

(defn calendar-local-type?
  "Returns true if dtype is a calendar-local type (lacks time zone information)."
  [dtype]
  (let [calendar-local-types #{:local-date :local-date-time :local-time}
        base-type (dt-packing/unpack-datatype dtype)]
    (boolean (calendar-local-types base-type))))

(defn normalize-target
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

(defn coerce-zone-id
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

(defn coerce-column
  "Coerce data to a column."
  [data]
  (if (tcc/column? data)
    data
    (tcc/column data)))
