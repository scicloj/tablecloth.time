(ns tablecloth.time.api.rolling-window
  (:import java.time.format.DateTimeParseException)
  (:import java.util.TreeMap)
  (:require [tablecloth.time.index :as time-index]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api :refer [slice]]
            [tech.v3.datatype.errors :as errors]
            [tick.alpha.api :as t]
            [tech.v3.dataset :as ds]))

;; rolling-window is modeled as a grouped dataset, a dataset of datasets
;;  sub datasets are slices on the orginal dataset and defined by rolling-window size


(defn- define-window
  "defines left and right indices of a rolling-window
    case 1: where we need the last n obs before loc (inclusive)"
  [n]
  (fn [loc]
    [(max 0 (+ (- loc n) 1)) loc]))


(defn- compute-windows
  "computes rolling-window indices for a each row of dataset"
  [ds n]
  (let [ds-row-indices (range (tablecloth/row-count ds))]
    (map (define-window n) ds-row-indices)))


(defn- slice-with-index
  "specializes slice to work on indices that are sequences and not date/time
   TODO: generalize and move to slice namespace"
  [index from to]
  (let [^TreeMap index index]
    (-> index
        (.subMap from true to true)
        (.values))))


(defn- define-slice
  "get slice (set of rows from dataset) from index for rolling-window"
  [index]
  (fn [[l r]]
    (-> index
        (slice-with-index l r)
        (vec))))

(defn- compute-slices
  "slices for each row of dataset
   using pre-computed windows with the index"
  [index windows]
  (map (define-slice index) windows))


(defn build-instructions
  "instructions as a map to dataset/group-by
  group-by build the dataset of datsets"
  [ds idx n]
  (let [windows (compute-windows ds n)
        slices (compute-slices idx windows)
        labels (map #(hash-map :loc (second %)) windows)]
    (zipmap labels slices)))

;; sample dataset to experiment with various type of indices
;; raw-index for simple sequence as index
;; raw-dates for dates as index
;; raw-alphas for group-by index
;; raw-values to show aggregation operations on grouped data

(def raw-index (range 10))
(def raw-values (map #(* 10 %) (take 10 (range))))
(def raw-alphas '("A" "AA" "AA" "AAA" "AAA" "AAA" "AAAA" "AAAA" "AAAA" "AAAA"))
(def raw-dates [(t/date "2021-01-10") (t/date "2021-01-11") (t/date "2021-01-12")
                (t/date "2021-01-13") (t/date "2021-01-14") (t/date "2021-01-15")
                (t/date "2021-01-16") (t/date "2021-01-17") (t/date "2021-01-18")
                (t/date "2021-01-19")])

(def ds (tablecloth/dataset [[:d-index raw-index]
                             [:d-values raw-values]
                             [:d-alphas raw-alphas]
                             [:d-dates raw-dates]]
                            {:dataset-name "ds"}))

;; create a index that is set into dataset's meta
(def ds-with-idx (tablecloth.time.index/index-by ds :d-index))

;; inspect index in meta
(meta ds-with-idx)

;; instructions for the rolling-window slices, so we can use group-by operation
(def instructions (build-instructions ds-with-idx (:index (meta ds-with-idx)) 5))

;; note - results are not ordered and so need an alternate impl of group-by
(-> ds-with-idx
    (tablecloth/group-by instructions)
    #_(vary-meta assoc :print-line-policy :repl))

;; create a index on dataset as a stand alone
(time-index/make-index ds :d-index)

;; we may need to support multiple indices on samedata set
(time-index/make-index ds :d-dates)

;; same dataset with different index in meta. is ds shared?
(time-index/index-by ds :d-dates)


