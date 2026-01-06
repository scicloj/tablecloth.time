(ns tablecloth.time.utils.binary-search
  (:import [java.util Collections])
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [tablecloth.column.api :as tcc]
            [tablecloth.api :as tc]))

(defn is-sorted?
  ([col]
   (is-sorted? col :ascending))
  ([col direction]
   (cond
     (or
      (empty? col)
      (= 1 (dtype/ecount col)))
     true
     (pos? (tcc/count-missing col))
     false
     :else
     (let [op (if (= direction :ascending) fun/>= fun/<=)

           shifted (fun/shift col -1)
           compared (op shifted col)]
       (= (dtype/ecount col)
          (fun/reduce-+ compared))))))

(defn ensure-time-column
  "Common helper for ds-level time operations.

  Given a dataset `ds` and a `time-col` keyword/name, this function:

  1. Ensures the column exists in the dataset.
  2. Optionally checks sortedness and sorts by `time-col` if needed.

  Options map:
  - :sorted?   boolean, default false. If true, skip the sortedness check.
  - :sort?     boolean, default true. If true and the column is not sorted,
               the dataset is sorted by `time-col` (ascending) before use.

  Returns a map {:ds ds' :time-col time-col :time-col-series col}, where
  - ds' is either the original ds or a sorted copy,
  - time-col-series is the (possibly reordered) column from ds'.

  This fn does *not* change the column's representation (no millis conversion).
  Representation conversion should be done explicitly by callers when needed."
  [ds time-col {:keys [sorted? sort?] :or {sorted? false
                                           sort?   true}}]
  (when-not (tc/has-column? ds time-col)
    (throw (ex-info (str "Time column not found: " time-col)
                    {:type      ::missing-time-column
                     :time-col  time-col
                     :col-names (tc/column-names ds)})))
  (let [col (tc/column ds time-col)
        ;; When caller promises sorted?, trust them and skip the check.
        sorted? (or sorted? (is-sorted? col :ascending))]
    (if (or sorted? (not sort?))
      {:ds ds
       :time-col time-col
       :time-col-series col
       :sorted? sorted?}
      (let [sorted-ds (tc/order-by ds time-col)
            sorted-col (tc/column sorted-ds time-col)]
        {:ds sorted-ds
         :time-col time-col
         :time-col-series sorted-col
         :sorted? true}))))

;; Collections/binarySearch encodes the insertion point using
;; the forumla `-(insertion-point) - 1`. This fn reverses that.
;; This gives us the place where the searched for value should
;; be inserted.
(defn- ->insertion-point [x]
  (- (inc x)))

(defn find-lower-bound [arr target]
  (if (zero? (count arr))
    0
    (let [result (Collections/binarySearch arr target)]
      (if (>= result 0)
        ;; exact match but we need to find the FIRST occurrence
        (loop [next-idx result]
          (if (or (zero? next-idx) (not= target (nth arr (dec next-idx))))
            next-idx
            (recur (dec next-idx))))
        (->insertion-point result)))))  ; insertion point

(defn find-upper-bound [arr target]
  (if (zero? (count arr))
    -1
    (let [result (Collections/binarySearch arr target)]
      (if (>= result 0)
        ;; exact match but we need to find the LAST occurrence
        (loop [next-idx result]
          (if (or (= (inc next-idx) (count arr)) (not= target (nth arr (inc next-idx))))
            next-idx
            (recur (inc next-idx))))
        (let [insertion-pt (->insertion-point result)]
          (if (zero? insertion-pt)
            -1
            (dec insertion-pt)))))))

