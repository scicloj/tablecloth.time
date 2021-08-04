(ns tablecloth.time.utils.indexing
  (:require [tablecloth.api :refer [columns]]
            [tablecloth.time.utils.validatable :refer [valid?]]
            [tablecloth.time.utils.datatypes :refer [get-datatype time-datatype?]]
            [tech.v3.datatype.casting :refer [datatype->object-class]]
            [tech.v3.datatype.packing :refer [unpack-datatype]]))

(defn time-column? [col]
  (time-datatype? (get-datatype col)))

(defn time-columns
  "Returns a list of columns containing time data, if any."
  [dataset]
  (filter time-column? (columns dataset)))

(defn index-column-name
  "Returns the name of the index column or `nil`. If the `:index` meta
  is set on the column metadata, that is the name of the index;
  otherwise, if there is a single column that can be identifed as time
  data, that will be the column name."
  [dataset]
  (if (valid? dataset :index)
    (-> dataset meta :validatable :index :column-names first)
    (if (= 1 (count (time-columns dataset)))
      (-> dataset time-columns first meta :name)
      nil)))

(defn index-column-object-class
  "Returns the object class of the index column data, if the index
  column is known."
  [dataset]
  (if-let [col-name (index-column-name dataset)]
    (-> (col-name dataset)
        get-datatype
        unpack-datatype
        datatype->object-class)
    nil))

(def unidentifiable-index-error
  (Exception. "Unable to auto detect time column to serve as index. Please specify the index using `index-by`."))

(defn can-identify-index-column?
  "Returns `true` or `false`, can the time index column be identified
  automatically?"
  [dataset]
  (boolean (index-column-name dataset)))

(defn get-index-column-name-or-error
  "Returns the time index column name if it can be identified."
  [dataset]
  (if (can-identify-index-column? dataset)
    (index-column-name dataset)
    (throw unidentifiable-index-error)))

(defn get-index-column-or-error
  "Returns the time index column of the dataset if it can be identified."
  [dataset]
  (if (can-identify-index-column? dataset)
    ((index-column-name dataset) dataset)
    (throw unidentifiable-index-error)))



