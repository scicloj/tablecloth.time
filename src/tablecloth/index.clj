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
  [index-column dataset]
  (with-meta dataset {:index (make-index dataset index-column)}))

(defn index-by [index-column dataset]
  {:index (make-index dataset index-column)
   :dataset dataset})


