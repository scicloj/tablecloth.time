(ns tablecloth.time.utils.datatypes
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.packing :as dt-packing]
            [tech.v3.datatype.datetime.base :as dtdt-base]))

(defn get-datatype
  "Get the unpacked datatype of a column.

  This is typically what you want when checking temporal types, as packed
  datatypes include storage metadata that obscures the semantic type."
  [col]
  (dt-packing/unpack-datatype (dtype/elemwise-datatype col)))

(defn temporal-type? [dtype]
  (let [dtype (dt-packing/unpack-datatype dtype)
        dtype-cat (dtdt-base/classify-datatype dtype)]
    (= dtype-cat :temporal)))

