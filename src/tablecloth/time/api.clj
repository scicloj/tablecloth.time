(ns tablecloth.time.api
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.column.api :as time-col]
            [tablecloth.time.time-literals :refer [modify-printing-of-time-literals-if-enabled!]]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dfn]))

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

;; Simple extractors (single field from datetime)
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
   :quarter      time-col/quarter
   :epoch-day    time-col/epoch-day
   :epoch-week   time-col/epoch-week})

;; Computed fields (derived from multiple extractors or require transformation)
(defn- hour-fractional
  "Hour of day as decimal (e.g., 13.5 for 13:30). Useful for sub-hourly data."
  [col]
  (dfn/+ (time-col/hour col)
         (dfn// (time-col/minute col) 60.0)))

(defn- daily-phase
  "Phase within day, normalized 0→1 (0=midnight, 0.5=noon, 1=midnight)."
  [col]
  (dfn// (hour-fractional col) 24.0))

(defn- weekly-phase
  "Phase within week, normalized 0→1 (0=Monday midnight, 1=Sunday midnight).
   Computed as: ((day-of-week - 1) * 24 + hour-fractional) / 168"
  [col]
  (let [dow (time-col/day-of-week col)   ; Monday=1, Sunday=7
        hf (hour-fractional col)
        hours-since-monday (dfn/+ (dfn/* (dfn/- dow 1) 24) hf)]
    (dfn// hours-since-monday 168.0)))

(defn- yearly-phase
  "Phase within year, normalized 0→1 (0=Jan 1, ~0.5=July 1, 1=Dec 31).
   Computed as: (day-of-year - 1) / 365"
  [col]
  (dfn// (dfn/- (time-col/day-of-year col) 1) 365.0))

(defn- week-index
  "Continuous week index (0-52) based on day-of-year.
   Avoids ISO week boundary issues where Jan 1 can be week 52/53."
  [col]
  (dfn// (dfn/- (time-col/day-of-year col) 1) 7))

(defn- date-string
  "Extract date portion as string (YYYY-MM-DD). Useful for grouping by day."
  [col]
  ;; Must use mapv for Java interop (.toLocalDate)
  (tcc/column (mapv #(str (.toLocalDate %)) col)))

(defn- year-string
  "Year as string. Useful for categorical coloring (avoids gradient)."
  [col]
  (tcc/column (dtype/emap str :string (time-col/year col))))

(defn- month-string
  "Month (1-12) as string. Useful for categorical coloring."
  [col]
  (tcc/column (dtype/emap str :string (time-col/month col))))

(defn- week-string
  "Week of year as string. Useful for categorical coloring."
  [col]
  (tcc/column (dtype/emap str :string (time-col/week-of-year col))))

(defn- day-of-week-string
  "Day of week (1-7) as string. Useful for categorical coloring."
  [col]
  (tcc/column (dtype/emap str :string (time-col/day-of-week col))))

(defn- year-week-string
  "Year and week as string 'YYYY-Www' for grouping weekly seasonal plots.
   Uses week-index (not ISO week) to avoid boundary issues."
  [col]
  ;; Must use mapv for format string
  (let [years (time-col/year col)
        weeks (week-index col)]
    (tcc/column (mapv (fn [y w] (str y "-W" (format "%02d" (int w)))) years weeks))))

(def ^:private field->computed
  {:hour-fractional    hour-fractional
   :daily-phase        daily-phase
   :weekly-phase       weekly-phase
   :yearly-phase       yearly-phase
   :week-index         week-index
   :date-string        date-string
   :year-string        year-string
   :month-string       month-string
   :week-string        week-string
   :day-of-week-string day-of-week-string
   :year-week-string   year-week-string})

(defn add-time-columns
  "Add columns extracted from a datetime column to a dataset.

  time-col — keyword or string name of the source datetime column
  fields   — vector of field keywords, or map of {field target-col-name}

  Supported fields:
    Basic:
      :year, :month, :day, :hour, :minute, :second,
      :day-of-week, :day-of-year, :week-of-year, :quarter,
      :epoch-day, :epoch-week

    Computed:
      :hour-fractional  — decimal hour (e.g., 13.5 for 13:30)
      :daily-phase      — position in day, 0→1 (0=midnight, 0.5=noon)
      :weekly-phase     — position in week, 0→1 (0=Monday 00:00)
      :yearly-phase     — position in year, 0→1 (0=Jan 1, ~0.5=July 1)
      :week-index       — continuous week (0-52), avoids ISO week boundary issues
      :date-string      — date as \"YYYY-MM-DD\" string (for grouping)
      :year-string      — year as string (for categorical color)
      :month-string     — month as string (for categorical color)
      :week-string      — week number as string (for categorical color)
      :day-of-week-string — day of week as string (for categorical color)
      :year-week-string — \"YYYY-Www\" format (for weekly seasonal grouping)

  Examples:
    ;; vector form — column names match field names
    (add-time-columns ds :Month [:year :month])
    ;; => adds :year and :month columns

    ;; map form — explicit output names
    (add-time-columns ds :Month {:year :Year, :month :MonthNum})
    ;; => adds :Year and :MonthNum columns

    ;; computed fields for seasonal plots
    (add-time-columns ds :Time {:daily-phase \"DailyPhase\"
                                 :date-string \"DateStr\"
                                 :year-string \"YearStr\"})"
  [ds time-col fields]
  (let [field->col (if (map? fields)
                     fields
                     (zipmap fields fields))
        all-extractors (merge field->extractor field->computed)
        src-col (ds time-col)]
    (tc/add-columns ds
      (reduce-kv (fn [m field col-name]
                   (if-let [extractor (all-extractors field)]
                     (assoc m col-name (extractor src-col))
                     (throw (ex-info (str "Unknown time field: " field
                                          ". Supported: " (keys all-extractors))
                                     {:field field}))))
                 {}
                 field->col))))

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
