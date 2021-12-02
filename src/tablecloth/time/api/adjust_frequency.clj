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
                        grouped dataset. Default: false
  - include-columns   - Additional columns to include when adjusting when
                        grouping at a new frequency.
  "
  ([dataset converter]
   (adjust-frequency dataset converter nil))
  ([dataset converter {:keys [include-columns ungroup? rename-index-to]
                       :or {ungroup? false}}]
   (let [index-column (get-index-column-or-error dataset)
         target-datatype (-> index-column first converter get-datatype)
         curr-column-name (-> index-column meta :name)
         next-column-name (or rename-index-to curr-column-name)
         adjusted-ds (cond-> dataset
                               (not= curr-column-name next-column-name)
                               (rename-index next-column-name)
                               :always
                               (tablecloth/update-columns
                                {next-column-name (partial emap converter target-datatype)}))]
     (if ungroup?
       adjusted-ds
       (tablecloth/group-by adjusted-ds (into [next-column-name] include-columns))))))
