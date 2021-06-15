(ns tablecloth.time.utils.datatypes
  (:require [tech.v3.datatype :refer [elemwise-datatype]]
            [tech.v3.datatype.datetime.packing :as datetime-packing]
            [tech.v3.datatype.datetime.base :as datetime-base]
            [clojure.set :refer [union]]))

(defn get-datatype
  "Returns the datatype keyword of `data` as identified by dtype-next's
  type system. If it is not a known type, it will return `:object`. `data`
  can be a column (or some type of 'reader' as defined by dtype-next) or
  an individual element of data."
  [data]
  (elemwise-datatype data))

(def time-datatypes
  (union datetime-packing/datatypes
         datetime-base/datatypes
         #{:year-quarter}))

(defn time-datatype? [dtype]
  (boolean (dtype time-datatypes)))

