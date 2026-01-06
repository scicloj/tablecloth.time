(ns tablecloth.time.api.slice
  (:require [tech.v3.datatype :as dtype]
            [tablecloth.api :as tc]
            [tablecloth.time.parse :as parse]
            [tablecloth.time.utils.datatypes :as types]
            [tablecloth.time.column.api :refer [convert-time]]
            [tablecloth.time.utils.binary-search :as bs]
            [tablecloth.column.api :as tcc]))

(set! *warn-on-reflection* true)

(defn- extract-key [key arg-name]
  (try
    (cond
      (or (int? key)
          (types/temporal-type? (types/get-datatype key)))
      key
      :else
      (parse/parse key))
    (catch Exception e
      (throw (ex-info (format "Unable to parse `%s` argument: %s" arg-name (.getMessage e))
                      {:argument arg-name
                       :value key
                       :cause e})))))

(defn- normalize-key [key arg-name]
  (try
    (first (convert-time [key] :epoch-milliseconds))
    (catch Exception e
      (throw (ex-info (format "Unable to convert `%s` to epoch milliseconds: %s" arg-name (.getMessage e))
                      {:argument arg-name
                       :value key
                       :cause e})))))

(defn- prepare-time-column-for-slice
  "Prepare a dataset's time column for slicing.

  - Ensures the column exists.
  - Converts it to epoch milliseconds.
  - Handles ascending vs descending input and returns a sorted millis column
    plus the sort direction.

  Returns a map {:col-millis col-millis-sorted :sort-direction dir}, where
  - col-millis-sorted is an epoch-millis column (ascending order),
  - dir is :ascending or :descending describing the *original* order."
  [ds time-column-name]
  (let [time-col (try
                   (get ds time-column-name)
                   (catch Exception e
                     (throw (ex-info (format "Unable to extract time column from dataset: %s"
                                             (.getMessage e))
                                     {:time-column time-column-name
                                      :cause e}))))]
    (when (nil? time-col)
      (throw (ex-info "Time column is nil or does not exist in dataset"
                      {:time-column time-column-name
                       :dataset-columns (vec (tc/column-names ds))})))
    (let [col-millis (try
                       (convert-time time-col :epoch-milliseconds)
                       (catch Exception e
                         (throw (ex-info (format "Unable to convert time column to epoch milliseconds: %s" (.getMessage e))
                                         {:time-column time-column-name
                                          :column-dtype (dtype/elemwise-datatype time-col)
                                          :cause e}))))
          sort-direction (if (<= (first col-millis) (last col-millis))
                           :ascending :descending)
          col-sorted? (bs/is-sorted? col-millis sort-direction)
          col-millis  (cond-> col-millis
                         (not col-sorted?)
                         (tcc/sort-column :asc)
                         (and col-sorted? (= sort-direction :descending))
                         (reverse))]
      {:col-millis     col-millis
       :sort-direction sort-direction})))

(defn slice
  "Returns a subset of dataset's rows between `from` and `to` (both inclusive).
  
  The time column is sliced using binary search, so it must be sorted in ascending
  order. 
  
  Arguments:
  - ds: the dataset to slice
  - time-column: keyword or function to select the time column (e.g., :timestamp or ds)
  - from: start time (string, datetime literal, epoch millis, or any temporal type)
  - to: end time (string, datetime literal, epoch millis, or any temporal type)
  - opts: optional map with:
    - :result-type - `:as-dataset` (default) returns a dataset, `:as-indices` returns indices
  
  Time values are normalized to epoch milliseconds for comparison. Strings are parsed
  using ISO-8601 format via `tablecloth.time.parse/parse`.
  
  Examples:
  
    ;; Basic usage with strings
    (slice ds :timestamp \"2024-01-01\" \"2024-12-31\")
    
    ;; With datetime literals (requires time-literals data readers)
    (slice ds :timestamp 
           #time/instant \"2024-01-01T00:00:00Z\" 
           #time/instant \"2024-12-31T23:59:59Z\")
    
    ;; Return indices instead of dataset
    (slice ds :timestamp \"2024-01-01\" \"2024-12-31\" {:result-type :as-indices})
    
    ;; Using epoch milliseconds directly
    (slice ds :timestamp 1704067200000 1704153599999)"
  ([ds time-column-name from to]
   (slice ds time-column-name from to nil))
  ([ds time-column-name from to {:keys [result-type]
                                 :or {result-type :as-dataset}}]
   (let [{:keys [col-millis sort-direction]} (prepare-time-column-for-slice ds time-column-name)
         from-key (-> from (extract-key "from") (normalize-key "from"))
         to-key (-> to (extract-key "to") (normalize-key "to"))
         _ (when (> from-key to-key)
             (throw (ex-info "The `from` time must be less than or equal to `to` time"
                             {:from from
                              :to to
                              :from-millis from-key
                              :to-millis to-key})))
         lower-bound (bs/find-lower-bound col-millis from-key)
         upper-bound (bs/find-upper-bound col-millis to-key)
         slice-indices (let [last-idx (dec (count col-millis))
                             range-seq (range
                                        (if (= sort-direction :descending)
                                          (- last-idx upper-bound)
                                          lower-bound)
                                        (inc
                                         (if (= sort-direction :descending)
                                           (- last-idx lower-bound)
                                           upper-bound)))]
                         (if (seq range-seq)
                           (dtype/->reader range-seq)
                           []))]
     (if (= result-type :as-indices)
       slice-indices
       (tc/select-rows ds slice-indices)))))
