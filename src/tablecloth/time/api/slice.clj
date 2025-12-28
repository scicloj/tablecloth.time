(ns tablecloth.time.api.slice
  (:require [tech.v3.datatype :as dtype]
            [tablecloth.api :as tc]
            [tablecloth.time.parse :as parse]
            [tablecloth.time.utils.datatypes :as types]
            [tablecloth.time.utils.binary-search :as binary-search]
            [tablecloth.time.column.api :refer [convert-time]]))

(set! *warn-on-reflection* true)

(defn- extract-key [key]
  (cond
    (or (int? key)
        (types/temporal-type? (types/get-datatype key)))
    key
    :else
    (parse/parse key)))

(defn- normalize-key [key]
  (first (convert-time [key] :epoch-milliseconds)))

(defn slice
  ([ds time-column from to]
   (let [col-idx (time-column ds)
         col-millis (convert-time col-idx :epoch-milliseconds)
         from-key (-> from extract-key normalize-key) 
         to-key (-> to extract-key normalize-key)
         lower-bound (binary-search/find-lower-bound col-millis from-key)
         upper-bound (binary-search/find-upper-bound col-millis to-key)
         slice-indices (dtype/->reader (range lower-bound (inc upper-bound)))]
     (tc/select-rows ds slice-indices))))

;; (defn slice
;;   "Returns a subset of dataset's rows (or row indexes) as specified by from and to, inclusively.
;;   `from` and `to` are either strings or datetime type literals (e.g. #time/local-date \"1970-01-01\").
;;   The dataset must have been indexed, and the time unit of the index must match the unit of time
;;   by which you are attempting to slice.

;;   Options are:

;;   - result-type - return results as dataset (`:as-dataset`, default) or a row of indexes (`:as-indexes`).

;;   Example data:

;;   |   :A | :B |
;;   |------|----|
;;   | 1970 |  0 |
;;   | 1971 |  1 |
;;   | 1972 |  2 |
;;   | 1973 |  3 |

;;   Example:

;;   (-> data
;;       (index-by :A)
;;       (slice \"1972\" \"1973\"))

;;   ;; => _unnamed [2 2]:

;;   |   :A | :B |
;;   |------|----|
;;   | 1972 |  2 |
;;   | 1973 |  3 |
;;   "
;;   ([dataset from to] (slice dataset from to nil))
;;   ([dataset from to {:keys [result-type]
;;                      :or {result-type :as-dataset}}]
;;    (let [build-err-msg (fn [^java.lang.Exception err arg-symbol time-unit]
;;                          (let [msg-str "Unable to parse `%s` date string. Its format may not match the expected format for the index time unit: %s. "]
;;                            (str (format msg-str arg-symbol time-unit) (.getMessage err))))
;;          index-column (get-index-column-or-error dataset)
;;          time-unit (unpack-datatype (get-datatype index-column))
;;          from-key (cond
;;                     (or (int? from)
;;                         (time-datatype? (get-datatype from))) from
;;                     :else (try
;;                             (string->time from)
;;                             (catch DateTimeParseException err
;;                               (throw (Exception. ^java.lang.String (build-err-msg err "from" time-unit))))))
;;          to-key (cond
;;                   (or (int? to)
;;                       (time-datatype? (get-datatype from))) to
;;                   :else (try
;;                           (string->time to)
;;                           (catch DateTimeParseException err
;;                             (throw (Exception. ^java.lang.String (build-err-msg err "to" time-unit))))))]
;;      (cond
;;        (not= time-unit (get-datatype from-key))
;;        (throw (Exception. (format "Time unit of `from` does not match index time unit: %s" time-unit)))
;;        (not= time-unit (get-datatype to-key))
;;        (throw (Exception. (format "Time unit of `to` does not match index time unit: %s" time-unit)))
;;        :else (let [index (index-structure index-column)
;;                    slice-indexes (select-from-index index :slice {:from from-key :to to-key})]
;;                (condp = result-type
;;                  :as-indexes slice-indexes
;;                  (select-rows dataset slice-indexes)))))))
