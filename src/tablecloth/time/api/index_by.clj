(ns tablecloth.time.api.index_by
  (:require [tablecloth.time.utils.validatable :refer [add-validatable]]))

(defn index-by
  "Identifies the column that should be used as the index for the
  dataset. Useful when functions that use the index to perform their
  operations, cannot auto-detect the index column. This can happen if
  there are more than one time-based column; or, if it is not clear
  that any column contains time data."
  [dataset index-column-name]
  (add-validatable dataset [index-column-name] :index nil))
