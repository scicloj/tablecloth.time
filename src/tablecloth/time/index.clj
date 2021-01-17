(ns tablecloth.time.index
  (:import java.util.TreeMap)
  (:require [tablecloth.api :as tablecloth]))

(set! *warn-on-reflection* true)

(defn get-index-meta [dataset]
  (-> dataset meta :index))

;; Better to name this get-index-key-type?
(defn get-index-type [dataset]
  (let [^TreeMap index (-> dataset get-index-meta)]
    (-> index .firstKey class)))

(defn make-index
  "Returns an index for `dataset` based on the specified `index-column-key`."
  [dataset index-column-key]
  (let [row-maps (-> dataset (tablecloth/rows :as-maps))
        idx-map (->> row-maps
                     (map-indexed (fn [row-number row-map]
                                    [(index-column-key row-map) row-number]))
                     (into {}))]
    (TreeMap. ^java.util.Map idx-map)))

(defn slice-index [dataset from to]
  (let [^TreeMap index (get-index-meta dataset)
        row-numbers (if (not index)
                      (throw (Exception. "Dataset has no index specified."))
                      (-> index (.subMap from true to true) (.values)))]
    (tablecloth/select-rows dataset row-numbers)))

(defn index-by
  "Returns a dataset with an index attached as metadata."
  [dataset index-column]
  (let [index (make-index dataset index-column)]
    (vary-meta dataset assoc :index index)))



