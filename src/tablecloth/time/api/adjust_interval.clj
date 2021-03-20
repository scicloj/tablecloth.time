(ns tablecloth.time.api.adjust-interval
  (:require [tech.v3.datatype :refer [emap elemwise-datatype]]
            [tablecloth.api :as tablecloth]))

(defn adjust-interval
  "Adjusts the interval of the dataset by adding a new column `new-column-name`
  whose values are the result of applying `time-converter` to the column specified
  by `index-column-name`, and then performing a `group-by` operation on that column
  and any columns specified by `keys`."
  [dataset index-column-name keys ->new-time-converter new-column-name]
  (let [index-column (index-column-name dataset)
        target-datatype (-> index-column first ->new-time-converter elemwise-datatype)
        adjusted-column-data (emap time-converter target-datatype index-column)]
    (-> dataset
        (tablecloth/add-or-replace-column new-column-name adjusted-column-data)
        (tablecloth/group-by (into [new-column-name] keys)))))
