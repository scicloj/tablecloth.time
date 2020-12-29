(ns tablecloth.time.index
  (:import java.util.TreeMap)
  (:require [tablecloth.api :as tablecloth]
            [tick.alpha.api :as t]))

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
  (let [index (make-index dataset index-column)]
    (vary-meta dataset assoc :index index)))

(defn get-index-meta [dataset]
  (-> dataset meta :index))

(defn slice [dataset from to]
  (let [index (get-index-meta dataset)
        row-numbers (-> index (.subMap from to) (.values))]
    (tablecloth/select-rows dataset row-numbers)))

(defn slice-year [dataset from to]
  (let [from-year (t/date (str from "-01-01"))
        to-year (t/date (str to "-01-01"))]
    (slice dataset from-year to-year)))

