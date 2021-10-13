(ns tablecloth.time.api.adjust-frequency
  (:require [tech.v3.datatype :refer [emap]]
            [tablecloth.time.utils.indexing :refer [get-index-column-or-error]]
            [tablecloth.time.utils.datatypes :refer [get-datatype]]
            [tablecloth.api :as tablecloth]))

(defn adjust-frequency
  "Adjusts the frequency of the time index column by applying the
  `converter` function to the values in the time index. Returns a
  grouped dataset that can be used with `tablecloth.api.aggregate`,
  for example.

  
  Options are:

  - ungroup? - Set to true if you want the function to return a
               grouped dataset. Default: true
  "
  ([dataset converter]
   (adjust-frequency dataset converter nil))
  ([dataset converter {:keys [include-columns ungroup?]
                       :or {ungroup? true}}]
   (let [index-column (get-index-column-or-error dataset)
         target-datatype (-> index-column first converter get-datatype)
         index-column-name (-> index-column meta :name)
         new-column-data (emap converter target-datatype index-column)
         adjusted-grouped-ds (-> dataset
                               (tablecloth/add-column index-column-name new-column-data)
                               (tablecloth/group-by (into [index-column-name] include-columns)))]
     (if ungroup?
       (tablecloth/ungroup adjusted-grouped-ds)
       adjusted-grouped-ds))))
