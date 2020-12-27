(ns tablecloth.time.index
  (:import java.util.TreeMap)
  (:require [tablecloth.api :as tablecloth]))

(defn make-index
  "Returns an index for `dataset` based on the specified `index-column-key`."
  [dataset index-column-key]
  (-> dataset
      (tablecloth/rows :as-maps)
      (->> (map-indexed (fn [row-number row-map]
                          [(index-column-key row-map) row-number]))
           (into {})
           (TreeMap.))))

(defn index-by
  "Returns a dataset with an index attached as metadata."
  [dataset index-column]
  (let [has-meta? (not= (meta dataset) nil)
        index (make-index dataset index-column)]
    (if has-meta?
      (vary-meta dataset assoc :index index)
      (with-meta dataset {:index index}))))



