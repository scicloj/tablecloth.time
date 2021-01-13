(ns tablecloth.time.index
  (:import java.util.TreeMap)
  (:require [tablecloth.api :as tablecloth]))

(set! *warn-on-reflection* true)

(defn make-index
  "Returns an index for `dataset` based on the specified `index-column-key`."
  [dataset index-column-key]
  (let [row-maps (-> dataset (tablecloth/rows :as-maps))
        idx-map (->> row-maps
                     (map-indexed (fn [row-number row-map]
                                    [(index-column-key row-map) row-number]))
                     (into {}))]
    (TreeMap. ^java.util.Map idx-map)))

(defn index-by
  "Returns a dataset with an index attached as metadata."
  [dataset index-column]
  (let [index (make-index dataset index-column)]
    (vary-meta dataset assoc :index index)))

(defn get-index-meta [dataset]
  (-> dataset meta :index))


