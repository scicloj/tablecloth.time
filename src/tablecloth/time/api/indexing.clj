(ns tablecloth.time.api.indexing
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.utils.validatable :refer [add-validatable has-validatable?]]
            [tablecloth.time.utils.indexing :refer [get-index-column-name-or-error]]))

(defn index-by
  "Identifies the column that should be used as the index for the
  dataset. Useful when functions that use the index to perform their
  operations, cannot auto-detect the index column. This can happen if
  there are more than one time-based column; or, if it is not clear
  that any column contains time data."
  [dataset index-column-name]
  (add-validatable dataset [index-column-name] :index nil))

(defn rename-index
  "Renames the index column if it can be identified."
  [dataset new-index-name]
  (let [current-index-name (get-index-column-name-or-error dataset)
        has-explicit-index? (has-validatable? dataset :index)]
    (some-> dataset
            (tablecloth/rename-columns
             {current-index-name new-index-name})
            (cond->
             has-explicit-index? (index-by new-index-name)
             (not has-explicit-index?) identity))))
