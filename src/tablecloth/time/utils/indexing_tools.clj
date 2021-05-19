(ns tablecloth.time.util.indexing-tools
  (:require [tablecloth.api :refer [columns]]
            [tech.v3.datatype :refer [elemwise-datatype]]
            [tech.v3.datatype.casting :refer [datatype->object-class]]
            [tech.v3.dataset.column :refer [index-structure index-structure-realized?]]
            [clojure.set :refer [union]]))


(tablecloth.time.time-literals/modify-printing-of-time-literals-if-enabled!)


(def time-datatypes
  (union tech.v3.datatype.datetime.packing/datatypes
         tech.v3.datatype.datetime.base/datatypes))


(defn time-datatype? [dtype]
  (boolean (dtype time-datatypes)))


(defn time-column? [col]
  (time-datatype? (elemwise-datatype col)))


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
    (elemwise-datatype (col-name dataset))
    nil))


(defn index-column-object-class [dataset]
  (if-let [col-name (index-column-name dataset)]
    (datatype->object-class (elemwise-datatype (col-name dataset)))
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


(comment
  (def ds (tbl/dataset {:A [#time/date "2010-01-01"
                            #time/date "2011-01-01"
                            #time/date "2012-01-01"]
                        :B [1 2 3]}))

  (tbl/columns ds)
  (:A ds)
  
  (->> (tbl/drop-columns ds :A)
       tbl/columns
       (filter #(-> % elemwise-datatype time-datatype?))
       first
       meta
       :name
       )
  )
