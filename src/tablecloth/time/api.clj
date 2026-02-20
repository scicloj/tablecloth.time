(ns tablecloth.time.api
  (:require [tablecloth.api :as tc]
            [tablecloth.time.column.api :as time-col]
            [tablecloth.time.time-literals :refer [modify-printing-of-time-literals-if-enabled!]]))

(modify-printing-of-time-literals-if-enabled!)

;; NOTE: The following legacy APIs have been removed and their tests archived:
;; - tablecloth.time.api.slice (slice, index-by - to be reimplemented)
;; - tablecloth.time.api.adjust-frequency (adjust-frequency - to be reimplemented)
;; - tablecloth.time.api.rolling-window (rolling-window - to be reimplemented)
;; - tablecloth.time.api.converters (various converters - functionality moved to column.api)
;; - tablecloth.time.api.time-components (field extractors - functionality moved to column.api)
;; See test/_archive/README.md for archived tests and development-plan.md for the new architecture.

;; -----------------------------------------------------------------------------
;; Dataset-level time operations
;; -----------------------------------------------------------------------------

(def ^:private field->extractor
  {:year         time-col/year
   :month        time-col/month
   :day          time-col/day
   :hour         time-col/hour
   :minute       time-col/minute
   :second       time-col/get-second
   :day-of-week  time-col/day-of-week
   :day-of-year  time-col/day-of-year
   :week-of-year time-col/week-of-year
   :quarter      time-col/quarter})

(defn add-time-columns
  "Add columns extracted from a datetime column to a dataset.

  time-col — keyword or string name of the source datetime column
  fields   — vector of field keywords, or map of {field target-col-name}

  Supported fields:
    :year, :month, :day, :hour, :minute, :second,
    :day-of-week, :day-of-year, :week-of-year, :quarter

  Examples:
    ;; vector form — column names match field names
    (add-time-columns ds :Month [:year :month])
    ;; => adds :year and :month columns

    ;; map form — explicit output names
    (add-time-columns ds :Month {:year :Year, :month :MonthNum})
    ;; => adds :Year and :MonthNum columns"
  [ds time-col fields]
  (let [field->col (if (map? fields)
                     fields
                     (zipmap fields fields))]
    (reduce-kv (fn [ds field col-name]
                 (if-let [extractor (field->extractor field)]
                   (tc/add-column ds col-name (extractor (ds time-col)))
                   (throw (ex-info (str "Unknown time field: " field
                                        ". Supported: " (keys field->extractor))
                                   {:field field}))))
               ds
               field->col)))

(defn add-lag
  "Add a lagged version of a column to a dataset.

  Creates a new column with values shifted forward by k positions,
  filling the first k positions with nil (tracked as missing values).

  Example:
    (add-lag ds :Beer 4 :Beer_lag4)
    ;; Adds column :Beer_lag4 where row i contains Beer[i-4], nil for i<4"
  [ds source-col k target-col]
  (tc/add-column ds target-col (time-col/lag (ds source-col) k)))

(defn add-lead
  "Add a lead (forward-shifted) version of a column to a dataset.

  Creates a new column with values shifted backward by k positions,
  filling the last k positions with nil (tracked as missing values).

  Example:
    (add-lead ds :Beer 4 :Beer_lead4)
    ;; Adds column :Beer_lead4 where row i contains Beer[i+4], nil for last 4 rows"
  [ds source-col k target-col]
  (tc/add-column ds target-col (time-col/lead (ds source-col) k)))

(defn add-lags
  "Add multiple lagged versions of a column to a dataset.

  lags — map of {k target-col-name} or vector of lags (auto-named as source_lag{k})

  Options:
    :drop-missing — drop rows with nil values in any of the new lag columns (default true)

  Examples:
    ;; Map form with custom names
    (add-lags ds :Beer {4 :Beer_lag4, 8 :Beer_lag8, 12 :Beer_lag12})

    ;; Vector form with auto-named columns (:Beer_lag1, :Beer_lag2, etc.)
    (add-lags ds :Beer [1 2 3 4])

    ;; Keep nils (don't drop rows)
    (add-lags ds :Beer [1 2 3 4] {:drop-missing false})"
  ([ds source-col lags]
   (add-lags ds source-col lags {}))
  ([ds source-col lags opts]
   (let [source-name (name source-col)
         make-col-name (if (string? source-col)
                         #(str source-name "_lag" %)
                         #(keyword (str source-name "_lag" %)))
         lag-map (cond
                   (map? lags) lags
                   (vector? lags) (into {} (map #(vector % (make-col-name %)) lags))
                   :else (throw (ex-info "lags must be a map or vector" {:lags lags})))
         ds-with-lags (reduce-kv (fn [d k target-col]
                                   (add-lag d source-col k target-col))
                                 ds
                                 lag-map)
         target-cols (vals lag-map)]
     (if (:drop-missing opts true)
       (tc/drop-missing ds-with-lags target-cols)
       ds-with-lags))))

(defn add-leads
  "Add multiple lead (forward-shifted) versions of a column to a dataset.

  leads — map of {k target-col-name} or vector of leads (auto-named as source_lead{k})

  Options:
    :drop-missing — drop rows with nil values in any of the new lead columns (default true)

  Examples:
    ;; Map form with custom names
    (add-leads ds :Beer {4 :Beer_lead4, 8 :Beer_lead8})

    ;; Vector form with auto-named columns (:Beer_lead1, :Beer_lead2, etc.)
    (add-leads ds :Beer [1 2 3 4])

    ;; Keep nils (don't drop rows)
    (add-leads ds :Beer [1 2 3 4] {:drop-missing false})"
  ([ds source-col leads]
   (add-leads ds source-col leads {}))
  ([ds source-col leads opts]
   (let [source-name (name source-col)
         make-col-name (if (string? source-col)
                         #(str source-name "_lead" %)
                         #(keyword (str source-name "_lead" %)))
         lead-map (cond
                    (map? leads) leads
                    (vector? leads) (into {} (map #(vector % (make-col-name %)) leads))
                    :else (throw (ex-info "leads must be a map or vector" {:leads leads})))
         ds-with-leads (reduce-kv (fn [d k target-col]
                                    (add-lead d source-col k target-col))
                                  ds
                                  lead-map)
         target-cols (vals lead-map)]
     (if (:drop-missing opts true)
       (tc/drop-missing ds-with-leads target-cols)
       ds-with-leads))))
