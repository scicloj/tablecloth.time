(ns tablecloth.time.utils.indexing-tools
  (:require [tablecloth.api :refer [columns]]
            [tablecloth.time.time-types :refer [additional-time-datatypes]]
            [tablecloth.time.utils.typing :refer [get-datatype]]
            [tech.v3.datatype.casting :refer [datatype->object-class]]
            [tech.v3.datatype.packing :refer [unpack-datatype packed-datatype?]]
            [tech.v3.dataset.column :refer [index-structure index-structure-realized?]]))


(defn time-column? [col]
  (time-datatype? (get-datatype col)))


(defn time-columns [dataset]
  (filter time-column? (columns dataset)))


(defn index-column-name [dataset]
  ;; if meta has index specified, then that is our index
  ;; if there's only on etime-column, that is our index
  ;; otherwise we have no index
  (if-let [idx-col-name (:index (meta dataset))]
    idx-col-name
    (if (= 1 (count (time-columns dataset)))
      (-> dataset time-columns first meta :name)
      nil))) 


(defn index-column-datatype [dataset]
  (if-let [col-name (index-column-name dataset)]
    (get-datatype (col-name dataset))
    nil))


(defn index-column-object-class [dataset]
  (if-let [col-name (index-column-name dataset)]
    (-> (col-name dataset)
        get-datatype
        unpack-datatype
        datatype->object-class)
    nil))


(defn can-identify-index-column? [dataset]
  (boolean (index-column-name dataset)))


(defn auto-detect-index-column [dataset]
  (if (can-identify-index-column? dataset)
    ((index-column-name dataset) dataset)))


(defn index-by
  "Returns a dataset with an index attached as metadata."
  [dataset index-column-name]
  (vary-meta dataset assoc :index index-column-name))


