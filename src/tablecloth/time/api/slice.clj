(ns tablecloth.time.api.slice
  (:require [tech.v3.datatype :as dtype]
            [tablecloth.api :as tc]
            [tablecloth.time.parse :as parse]
            [tablecloth.time.utils.datatypes :as types]
            [tablecloth.time.utils.binary-search :as binary-search]
            [tablecloth.time.column.api :refer [convert-time]]))

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

;; TODO: Add sortedness check, and then later add a `:sorted?` option
;; to skip the sortedness check.
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
  ([ds time-column from to]
   (slice ds time-column from to nil))
  ([ds time-column from to {:keys [result-type]
                            :or {result-type :as-dataset}}]
   (let [col-idx (try
                   (time-column ds)
                   (catch Exception e
                     (throw (ex-info (format "Unable to extract time column from dataset: %s" (.getMessage e))
                                     {:time-column time-column
                                      :cause e}))))
         _ (when (nil? col-idx)
             (throw (ex-info "Time column is nil or does not exist in dataset"
                             {:time-column time-column
                              :dataset-columns (vec (tc/column-names ds))})))
         col-millis (try
                      (convert-time col-idx :epoch-milliseconds)
                      (catch Exception e
                        (throw (ex-info (format "Unable to convert time column to epoch milliseconds: %s" (.getMessage e))
                                        {:time-column time-column
                                         :column-dtype (dtype/elemwise-datatype col-idx)
                                         :cause e}))))
         from-key (-> from (extract-key "from") (normalize-key "from"))
         to-key (-> to (extract-key "to") (normalize-key "to"))
         _ (when (> from-key to-key)
             (throw (ex-info "The `from` time must be less than or equal to `to` time"
                             {:from from
                              :to to
                              :from-millis from-key
                              :to-millis to-key})))
         lower-bound (binary-search/find-lower-bound col-millis from-key)
         upper-bound (binary-search/find-upper-bound col-millis to-key)
         slice-indices (dtype/->reader (range lower-bound (inc upper-bound)))]
     (if (= result-type :as-indices)
       slice-indices
       (tc/select-rows ds slice-indices)))))

