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

(defn slice-index
  "Returns a subset of dataset's rows (or row indexes) as specified by from and to, inclusively.

  Options are:

  - result-type - return results as dataset (`:as-dataset`, default) or a row of indexes (`:as-indexes`). "
  ([dataset from to] (slice-index dataset from to nil))
  ([dataset from to {:keys [result-type]
                     :or {result-type :as-dataset}
                     :as options}]
   (let [^TreeMap index (get-index-meta dataset)
         row-numbers (if (not index)
                       (throw (Exception. "Dataset has no index specified."))
                       (-> index (.subMap from true to true) (.values)))]
     (condp = result-type
       :as-indexes row-numbers
       (tablecloth/select-rows dataset row-numbers)))))

(defn index-by
  "Returns a dataset with an index attached as metadata."
  [dataset index-column]
  (let [index (make-index dataset index-column)]
    (vary-meta dataset assoc :index index)))



