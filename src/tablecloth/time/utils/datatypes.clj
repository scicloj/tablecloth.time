(ns tablecloth.time.utils.datatypes
  (:require [tech.v3.datatype :refer [elemwise-datatype]]
            [clojure.set :refer [union]]))

(defn get-datatype [data]
  "Returns the datatype keyword of `data` as identified by dtype-next's
type system. If it is not a known type, it will return `:object`. `data`
can be a column (or some type of 'reader' as defined by dtype-next) or
an individual element of data."
  (elemwise-datatype data))

(def time-datatypes
  (union tech.v3.datatype.datetime.packing/datatypes
         tech.v3.datatype.datetime.base/datatypes))

(defn time-datatype? [dtype]
  (boolean (dtype time-datatypes)))

