(ns tablecloth.time.api.slice
  (:import java.time.format.DateTimeParseException)
  (:require [tablecloth.time.utils.indexing :refer [get-index-column-or-error]]
            [tablecloth.time.utils.datatypes :refer [get-datatype time-datatype?]]
            [tablecloth.api :refer [select-rows]]
            [tech.v3.dataset.column :refer [index-structure]]
            [tech.v3.dataset.column-index-structure :refer [select-from-index]]
            [tech.v3.datatype.packing :refer [unpack-datatype]]))

(set! *warn-on-reflection* true)

;; These parsing tools should probably become part of our own
;; parsing fn.
(defmulti parse-datetime-str
  (fn [datetime-datatype _] datetime-datatype))

(defmethod parse-datetime-str :instant
  [_ date-str]
  (java.time.Instant/parse date-str))

(defmethod parse-datetime-str :local-date
  [_ date-str]
  (java.time.LocalDate/parse date-str))

(defmethod parse-datetime-str :local-date-time
  [_ date-str]
  (java.time.LocalDateTime/parse date-str))

;; Leaving these around for when we generalize a parsing fn
;; Commenting out for now b/c we aren't sure about support for
;; these other types consdering our use of tech.datatype.datetime
;; that doesn't support these. See this conversation:
;; https://clojurians.zulipchat.com/#narrow/stream/236259-tech.2Eml.2Edataset.2Edev/topic/extra.20datetime.20types/near/239699398

;; (defmethod parse-datetime-str java.time.ZonedDateTime
;;   [_ date-str]
;;   (java.time.ZonedDateTime/parse date-str))

;; (defmethod parse-datetime-str java.time.YearMonth
;;   [_ date-str]
;;   (java.time.YearMonth/parse date-str))

;; (defmethod parse-datetime-str java.time.Year
;;   [_ date-str]
;;   (java.time.Year/parse date-str))

(defn slice
  "Returns a subset of dataset's rows (or row indexes) as specified by from and to, inclusively.
  `from` and `to` are either strings or datetime type literals (e.g. #time/local-date \"1970-01-01\").
  The dataset must have been indexed, and the time unit of the index must match the unit of time
  by which you are attempting to slice.

  Options are:

  - result-type - return results as dataset (`:as-dataset`, default) or a row of indexes (`:as-indexes`).

  Example data:

  |   :A | :B |
  |------|----|
  | 1970 |  0 |
  | 1971 |  1 |
  | 1972 |  2 |
  | 1973 |  3 |

  Example:

  (-> data
      (index-by :A)
      (slice \"1972\" \"1973\"))

  ;; => _unnamed [2 2]:

  |   :A | :B |
  |------|----|
  | 1972 |  2 |
  | 1973 |  3 |
  "
  ([dataset from to] (slice dataset from to nil))
  ([dataset from to {:keys [result-type]
                     :or {result-type :as-dataset}}]
   (let [build-err-msg (fn [^java.lang.Exception err arg-symbol time-unit]
                         (let [msg-str "Unable to parse `%s` date string. Its format may not match the expected format for the index time unit: %s. "]
                           (str (format msg-str arg-symbol time-unit) (.getMessage err))))
         index-column (get-index-column-or-error dataset)
         time-unit (unpack-datatype (get-datatype index-column))
         from-key (cond
                    (or (int? from)
                        (time-datatype? (get-datatype from))) from
                    :else (try
                            (parse-datetime-str (unpack-datatype time-unit) from)
                            (catch DateTimeParseException err
                              (throw (Exception. ^java.lang.String (build-err-msg err "from" time-unit))))))
         to-key (cond
                  (or (int? to)
                      (time-datatype? (get-datatype from))) to
                  :else (try
                          (parse-datetime-str (unpack-datatype time-unit) to)
                          (catch DateTimeParseException err
                            (throw (Exception. ^java.lang.String (build-err-msg err "to" time-unit))))))]
     (cond
       (not= time-unit (get-datatype from-key))
       (throw (Exception. (format "Time unit of `from` does not match index time unit: %s" time-unit)))
       (not= time-unit (get-datatype to-key))
       (throw (Exception. (format "Time unit of `to` does not match index time unit: %s" time-unit)))
       :else (let [index (index-structure index-column)
                   slice-indexes (select-from-index index :slice {:from from-key :to to-key})]
               (condp = result-type
                 :as-indexes slice-indexes
                 (select-rows dataset slice-indexes)))))))
