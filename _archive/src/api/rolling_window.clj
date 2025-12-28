(ns tablecloth.time.api.rolling-window
  (:import [java.util Map LinkedHashMap])
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.api :refer [slice]]
            [tablecloth.time.utils.indexing :refer [get-index-column-name-or-error]]))


;; A rolling-window for a row is defined by it's length, and maps to a set of rows preceding it
;; We use a grouped dataset, which is a dataset of datasets, to hold the rows of a rolling-window

;; notation
;; loc is row location in a column
;; window-size is the number of rows in the dataset. same as row-count
;; index-name is column name in the dataset that is used as the index
;; left & right are the corresponding edges of a window

;; notes
;; The implementation is simple. We leverage tablecloth/group-by functionality
;; that takes in a map of row-indices to group by, and creates the grouped dataset
;; The instructions function builds the map of rolling-window indices


(defn- rw-index
  "left/right edges of a rolling window for a given location
  case #1: column is an ordered seqeunce, and so computes window edges"
  [window-size]
  (fn [loc]
    [(max 0 (+ (- loc window-size) 1)) loc]))

(defn- rw-index-values
  "column values at left/right edges of a rolling window"
  [ds column-name]
  (fn [loc]
    (let [[left right] loc
          column (tablecloth/column ds column-name)]
      [(nth column left) (nth column right)])))

(defn- rw-indices
  "rolling-window left/right indices for a dataset"
  [ds column-name window-size]
  (let [count (tablecloth/row-count ds)
        indices (take count (range))]
    (->> (map (rw-index window-size) indices)
         (map (rw-index-values ds column-name)))))

(defn- rw-slice
  "slice (set of row indices from dataset) for a rolling-window"
  [ds]
  (fn [[left right]]
    (-> ds
        (slice left right {:result-type :as-indexes})
        (vec))))


;; for symmetry. slice (row) and slices (dataset)


(defn- rw-slices
  "slices for a dataset"
  [ds indices]
  (map (rw-slice ds) indices))


;; utility


(defn- ->ordered-map
  "creates an ordered map {k, v} from ordered inputs [k] [v])"
  [k v]
  (let [om (LinkedHashMap.)
        kv (mapv vector k v)]
    (doseq [[ke ve] kv]
      (.put ^Map om ke ve))
    om))

(defn- rw-map
  "maps row location to row indices of it's rolling-window.
   it is used as an input to group-by to create a grouped dataset"
  [ds column-name window-size]
  (let [indices (rw-indices ds column-name window-size)
        slices (rw-slices ds indices)
        labels (map #(hash-map :loc (second %)) indices)]
    (->ordered-map labels slices)))


;; support alternate approaches to build grouped datasets for rolling-window


;; (defn rolling-window
;;   "rolling-window dataset"
;;   [ds column-name window-size]
;;   (tablecloth/group-by ds (rw-map ds column-name window-size)))


(defn rolling-window
  "rolling-windows for a dataset"
  [ds window-size]
  (let [column-name (-> ds get-index-column-name-or-error)]
    (tablecloth/group-by ds (rw-map ds column-name window-size))))



