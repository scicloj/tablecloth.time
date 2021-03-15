(ns tablecloth.time.api.adjust-interval
  (:require [tech.v3.datatype :refer [emap elemwise-datatype]]
            [tablecloth.api :as tablecloth]))

(defn adjust-interval
  "Change the time index frequency."
  [dataset index-column-name keys time-converter new-column-name]
  (let [index-column (index-column-name dataset)
        target-datatype (-> index-column first time-converter elemwise-datatype)
        adjusted-column-data (emap time-converter target-datatype index-column)]
    (-> dataset
        (tablecloth/add-or-replace-column new-column-name adjusted-column-data)
        (tablecloth/group-by (into [new-column-name] keys)))))
