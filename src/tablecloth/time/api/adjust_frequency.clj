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

  - include-columns - other columns (in addition to the time index) by
                      that should be grouped together at the specified
                      interval.

    Example Data:
  
    |         :A |  :B | :C |
    |------------|-----|---:|
    | 1970-01-01 | foo |  0 |
    | 1970-01-02 | foo |  1 |
    | 1970-02-01 | foo |  2 |
    | 1970-02-02 | bar |  3 |

    (-> data
        (index-by :A)
        (adjust-frequency ->months-end {:include-columns [:B]})
        (tablecloth.api/aggregate (comp tech.v3.datatype.functional/mean :C)))
    ;; => _unnamed [3 3]:
    ;;    | :summary |  :B |         :A |
    ;;    |---------:|-----|------------|
    ;;    |      0.5 | foo | 1970-01-31 |
    ;;    |      2.0 | foo | 1970-02-28 |
    ;;    |      3.0 | bar | 1970-02-28 |
  "
  ([dataset converter]
   (adjust-frequency dataset converter nil))
  ([dataset converter {:keys [include-columns]}]
   (let [index-column (get-index-column-or-error dataset)
         target-datatype (-> index-column first converter get-datatype)
         index-column-name (-> index-column meta :name)
         new-column-data (emap converter target-datatype index-column)]
     (-> dataset
         (tablecloth/add-column index-column-name new-column-data)
         (tablecloth/group-by (into [index-column-name] include-columns))))))
