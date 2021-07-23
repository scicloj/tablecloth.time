(ns tablecloth.time.api.adjust-interval
  (:require [tech.v3.datatype :refer [emap]]
            [tablecloth.time.utils.indexing :refer [get-index-column-or-error]]
            [tablecloth.time.utils.datatypes :refer [get-datatype]]
            [tablecloth.api :as tablecloth]))

;; (defn- do-adjust-interval
;;   [dataset index-column-name keys ->new-time-converter new-column-name]
;;   (let [index-column (index-column-name dataset)
;;         target-datatype (-> index-column first ->new-time-converter elemwise-datatype)
;;         adjusted-column-data (emap ->new-time-converter target-datatype index-column)]
;;     (-> dataset
;;         (tablecloth/add-or-replace-column new-column-name adjusted-column-data)
;;         (tablecloth/group-by (into [new-column-name] keys)))))


(defn adjust-interval 
  "Adjusts the interval of the time index column by applying the
  `converter` function to the values in the time index. Returns a
  grouped dataset that can be used with `tablecloth.api.aggregate`,
  for example."
  ([dataset converter]
   (adjust-interval dataset converter nil))
  ([dataset converter {:keys [also-group-by]}]
   (let [index-column (get-index-column-or-error dataset)
         target-datatype (-> index-column first converter get-datatype)
         index-column-name (-> index-column meta :name)
         new-column-name (if rename-index-to 
                           rename-index-to
                           index-column-name)
         new-column-data (emap converter target-datatype index-column)]
     (-> dataset
         (tablecloth/add-column new-column-name new-column-data)
         (tablecloth/group-by (into [new-column-name] also-group-by))))))
