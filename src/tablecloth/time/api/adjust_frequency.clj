(ns tablecloth.time.api.adjust-frequency
  (:require [tech.v3.datatype :refer [emap]]
            [tablecloth.time.api.indexing :refer [rename-index]]
            [tablecloth.time.utils.indexing :refer [get-index-column-or-error]]
            [tablecloth.time.utils.datatypes :refer [get-datatype]]
            [tablecloth.api :as tablecloth]))

(defn adjust-frequency
  "Adjusts the frequency of the time index column by applying the
  `converter` function to the values in the time index. Returns a
  grouped dataset that can be used with `tablecloth.api.aggregate`,
  for example.

  
  Options are:
  - rename-index-to   - Rename the index column name 
  - ungroup?          - Set to true if you want the function to return a
                        grouped dataset. Default: true
  - include-columns   - Additional columns to include when adjusting when
                        grouping at a new frequency.
  "
  ([dataset converter]
   (adjust-frequency dataset converter nil))
  ([dataset converter {:keys [include-columns ungroup? rename-index-to]
                       :or {ungroup? true}}]
   (let [index-column (get-index-column-or-error dataset)
         target-datatype (-> index-column first converter get-datatype)
         index-column-name (-> index-column meta :name)
         new-column-data (emap converter target-datatype index-column)
         adjusted-grouped-ds (cond-> dataset
                               (some? rename-index-to)
                               (rename-index rename-index-to)
                               :always
                               (tablecloth/group-by
                                (into [(or rename-index-to index-column-name)] include-columns)))]
     (if ungroup?
       (tablecloth/ungroup adjusted-grouped-ds)
       adjusted-grouped-ds))))
