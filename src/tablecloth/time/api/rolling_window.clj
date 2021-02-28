(ns tablecloth.time.api.rolling-window
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as tidx]
            [tablecloth.time.api :refer [slice]]))

;; A rolling-window is of size len
;; A dataset is constructed to hold the rows of a rolling-window

;; For each row of the dataset, a rolling-window is defined
;; So, rolling-window dataset is essentially a dataset of datasets - a grouped dataset

;; Analytic functions operating on a grouped dataset are available for rolling-window

;; inputs #1: an indexed dataset, eolling window size
;; TODO: inputs #2: index, dataset row count, rolling window size

(defn- rw-index
  "rolling-window left/right indices for a row
    case 1: where we need the last n obs before loc (inclusive)"
  [len]
  (fn [loc]
    [(max 0 (+ (- loc len) 1)) loc]))


(defn- rw-indices
  "rolling-window left/right indices for a dataset"
  [ds len]
  (let [count (tablecloth/row-count ds)
        indices (take count (range))]
    (map (rw-index len) indices)))


(defn- rw-slice
  "slice (set of row indices from dataset) for a rolling-window"
  [ds]
  (fn [[left right]]
    (-> ds
        (slice left right {:result-type :as-indexes})
        (vec))))

;; for symmetry
(defn- rw-slices
  "slices for a dataset"
  [ds indices]
  (map (rw-slice ds) indices))

(defn instructions
  "maps row location to row indices of it's rolling-window.
   it will be used as a group-by input to create a grouped dataset"
  [ds len]
  (let [indices (rw-indices ds len)
        slices (rw-slices ds indices)
        labels (map #(hash-map :loc (second %)) indices)]
    (zipmap labels slices)))

;; support alternate approaches to build grouped datasets for rolling-window
(defn rolling-window
  "entry for a rolling-window dataset"
  [ds len]
  (tablecloth/group-by ds (instructions ds len)))



