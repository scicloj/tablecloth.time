;; # Chapter 2: Time Series Graphics
;; Translating fpp3 Chapter 2 examples from R to Clojure
;; using tablecloth, tablecloth.time, and tableplot.
;;
;; Reference: https://otexts.com/fpp3/graphics.html
;;
;; Run with: `clj -A:notebooks` then evaluate in your editor,
;; or render with Clay: `(clay/make! {:source-path "notebooks/chapter_02_time_series_graphics.clj"})`

(ns chapter-02-time-series-graphics
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as dfn]
            [tech.v3.dataset :as ds]
            [scicloj.tableplot.v1.plotly :as plotly]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.time.api :as time-api]
            [tablecloth.time.column.api :as time-col])
  (:import [java.time LocalDate]))

;; ## 2.1 — Loading data (tsibble equivalents)
;;
;; In R, fpp3 provides datasets as tsibble objects with declared index
;; and keys. In Clojure, we use plain tablecloth datasets loaded from CSV.
;; The time column is just a column — no special metadata needed.

;; ### Helper: load fpp3 dataset
(defn load-fpp3
  "Load one of the fpp3 datasets from CSV."
  [name]
  (tc/dataset (str "data/fpp3/" name ".csv")))

;; ### PBS — Australian pharmaceutical benefit scheme
;; R: PBS (67,596 × 9, monthly, keyed by Concession/Type/ATC1/ATC2)

(def PBS (load-fpp3 "PBS"))

PBS

;; ### The a10 pipeline
;; In R:
;; ```r
;; PBS |>
;;   filter(ATC2 == "A10") |>
;;   select(Month, Concession, Type, Cost) |>
;;   summarise(TotalC = sum(Cost)) |>
;;   mutate(Cost = TotalC / 1e6) -> a10
;; ```

(def a10
  (-> PBS
      (tc/select-rows #(= "A10" (get % "ATC2")))
      (tc/select-columns ["Month" "Concession" "Type" "Cost"])
      (tc/group-by ["Month"])
      (tc/aggregate {"TotalC" #(dfn/sum (% "Cost"))})
      (tc/add-column "Cost" #(dfn// (% "TotalC") 1e6))))

a10

;; ### ansett — Ansett airlines weekly passenger data
(def ansett (load-fpp3 "ansett"))
ansett

;; ### aus_production — Australian quarterly production
(def aus-production (load-fpp3 "aus_production"))
aus-production

;; ### vic_elec — Victorian half-hourly electricity demand
;; Time column is "2011-12-31 13:00:00" — not auto-parsed by tablecloth.
;; Use tc/convert-types with a format pattern to parse it.
(def vic-elec
  (-> (load-fpp3 "vic_elec")
      (tc/convert-types "Time" [:local-date-time "yyyy-MM-dd HH:mm:ss"])))
vic-elec

;; ### olympic_running — Olympic running times
(def olympic-running (load-fpp3 "olympic_running"))
olympic-running

;; ## 2.2 — Time plots
;;
;; R: `autoplot(melsyd_economy, Passengers)`
;; Clojure: tableplot line chart

;; ### Ansett airlines: Melbourne-Sydney economy class
(def melsyd-economy
  (-> ansett
      (tc/select-rows #(and (= "MEL-SYD" (get % "Airports"))
                            (= "Economy" (get % "Class"))))
      (tc/add-column "Passengers (000s)" #(dfn// (% "Passengers") 1000))))

(-> melsyd-economy
    (plotly/layer-line {:=x "Week"
                        :=y "Passengers (000s)"
                        :=title "Ansett airlines economy class: Melbourne-Sydney"}))

;; Notable features visible in the plot:
;; - 1989: No passengers (industrial dispute)
;; - 1992: Reduced load (economy seats replaced by business class trial)
;; - Late 1991: Large increase in passenger load
;; - Start of each year: Holiday-effect dips

;; ### Antidiabetic drug sales (a10)
(-> a10
    (plotly/layer-line {:=x "Month"
                        :=y "Cost"
                        :=title "Australian antidiabetic drug sales"
                        :=y-title "$ (millions)"}))

;; Clear increasing trend with strong seasonality that grows proportionally.
;; The January spike each year is from stockpiling before year-end subsidies.

;; ## 2.3 — Time series patterns
;;
;; Three fundamental components:
;; - **Trend**: long-term increase/decrease
;; - **Seasonal**: fixed, known period (calendar-linked)
;; - **Cyclic**: rises/falls of variable, non-fixed frequency
;;
;; The four examples from Figure 2.3:

;; ### Australian quarterly beer production (seasonal + no trend)
(def recent-beer
  (-> aus-production
      (tc/select-rows #(>= (.getYear (get % "Quarter")) 2000))
      (tc/select-columns ["Quarter" "Beer"])))

(-> recent-beer
    (plotly/layer-line {:=x "Quarter"
                        :=y "Beer"
                        :=title "Australian quarterly beer production"}))

;; ### Google stock — daily changes (no pattern, random walk)
(def gafa (load-fpp3 "gafa_stock"))

(def google-2015
  (-> gafa
      (tc/select-rows #(and (= "GOOG" (get % "Symbol"))
                            (= (.getYear (get % "Date")) 2015)))))

;; Daily closing price
(-> google-2015
    (plotly/layer-line {:=x "Date"
                        :=y "Close"
                        :=title "Google daily closing stock price (2015)"}))

;; Daily *change* in closing price
(let [closes (vec (google-2015 "Close"))
      diffs (mapv - (rest closes) (butlast closes))]
  (-> (tc/dataset {"Date" (rest (vec (google-2015 "Date")))
                    "Change" diffs})
      (plotly/layer-line {:=x "Date"
                          :=y "Change"
                          :=title "Google daily change in closing stock price (2015)"})))

;; ## 2.4 — Seasonal plots
;;
;; R: `gg_season(a10, Cost)` — overlay each year on the same month axis.
;; We extract year and month, then plot with color = year.

;; tablecloth.time has `add-time-columns` — a dataset-level operation
;; that extracts datetime fields in one call. tablecloth auto-parses
;; our CSV date strings into :packed-local-date, so these work out of the box.
;;
;; Vector form: column names match field names
;; Map form: explicit output names

(def a10-seasonal
  (-> a10
      (time-api/add-time-columns "Month" {:year "Year" :month "MonthNum"})))

;; Year is int64 — tableplot treats numeric columns as continuous color scales.
;; Convert to string so it's treated as categorical (one line per year).
(-> a10-seasonal
    (tc/add-column "YearStr" #(mapv str (% "Year")))
    (plotly/layer-line {:=x "MonthNum"
                        :=y "Cost"
                        :=color "YearStr"
                        :=title "Seasonal plot: Antidiabetic drug sales"
                        :=x-title "Month"
                        :=y-title "$ (millions)"}))

;; Each line is one year. The seasonal shape is clear:
;; - January spike (stockpiling)
;; - Generally higher in second half of year
;; - The whole pattern shifts upward year over year (trend)

;; ### Multiple seasonal periods — vic_elec
;; Electricity demand has daily, weekly, and yearly patterns.
;; R: `gg_season(Demand, period = "day"|"week"|"year")`

;; Now that Time is parsed as LocalDateTime, we can use add-time-columns directly.
;; The new computed fields handle fractional hours, phases, and string conversions.
;; NOTE: The "Date" column is the billing/reporting date (next day), not the
;; calendar date of the timestamp. We derive all groupings from the Time column.
(def vic-elec-with-fields
  (-> vic-elec
      (time-api/add-time-columns "Time" 
        {;; Basic fields
         :day-of-week "DayOfWeek"
         :day-of-year "DayOfYear"
         :week-of-year "WeekOfYear"
         :year "Year"
         ;; Computed fields for seasonal plots
         :hour-fractional "HourOfDay"
         :daily-phase "DailyPhase"
         :weekly-phase "WeeklyPhase"
         :week-of-year-index "WeekIndex"
         :date-string "TimeDate"
         :year-string "YearStr"
         :week-string "WeekLabel"
         :year-week-string "YearWeek"})))

;; ### Helper: seasonal-plot-spec
;; Generate a Plotly spec for seasonal plots using tableplot as the base.
;; This uses tableplot's layer-line with :=color to generate multiple traces,
;; then post-processes to hide legend and set custom colors.
(defn seasonal-plot-spec
  "Generate a Plotly spec for a seasonal plot.
   - ds: dataset (should include phase column)
   - phase-col: column for x-axis (phase within period, 0 to 1)
   - value-col: column for y-axis  
   - group-col: column to group by (creates one trace per unique value)
   - color-fn: fn from group-name (string) -> color string
   Options:
   - :line-width (default 0.3)
   - :title, :x-title, :y-title for axis labels"
  [ds phase-col value-col group-col color-fn 
   & {:keys [line-width title x-title y-title]
      :or {line-width 0.3}}]
  (let [;; Use tableplot to generate base spec with traces
        viz (-> ds
                (tc/order-by phase-col)
                (plotly/layer-line {:=x phase-col 
                                    :=y value-col 
                                    :=color group-col
                                    :=title title
                                    :=x-title x-title
                                    :=y-title y-title}))
        ;; Extract final Plotly spec using tableplot's official API
        spec (plotly/plot viz)]
    ;; Post-process traces: hide legend, set colors
    (update spec :data 
            #(mapv (fn [trace]
                     (-> trace
                         (assoc :showlegend false)
                         (assoc-in [:line :color] (color-fn (:name trace)))
                         (assoc-in [:line :width] line-width)))
                   %))))

;; Daily pattern: phase = hour/24, each day overlaid.
;; Using seasonal-plot-spec helper with tableplot as base.
(let [year-color #(get {"2011" "#7570b3" "2012" "#1b9e77" "2013" "#d95f02" "2014" "#7570b3"}
                       (subs % 0 4) "gray")]
  (kind/plotly
    (seasonal-plot-spec vic-elec-with-fields
                        "DailyPhase" "Demand" "TimeDate"
                        year-color
                        :title "Electricity demand: Victoria (daily pattern)"
                        :x-title "Phase of day (0=midnight, 0.5=noon)"
                        :y-title "MWh")))

;; Weekly pattern: phase = hours_since_monday / 168, each week overlaid.
;; Using seasonal-plot-spec helper with tableplot as base.
(let [year-color #(get {"2011" "#7570b3" "2012" "#1b9e77" "2013" "#d95f02" "2014" "#7570b3"}
                       (subs % 0 4) "gray")]
  (kind/plotly
    (seasonal-plot-spec vic-elec-with-fields
                        "WeeklyPhase" "Demand" "YearWeek"
                        year-color
                        :title "Electricity demand: Victoria (weekly pattern)"
                        :x-title "Phase of week (0=Mon, 0.5=Thu noon, 1=Sun midnight)"
                        :y-title "MWh")))

;; Yearly pattern: x = day of year, each year is a line.
;; All 3 years fit — only 3 traces.
(-> vic-elec-with-fields
    (plotly/layer-line {:=x "DayOfYear"
                        :=y "Demand"
                        :=color "YearStr"
                        :=title "Electricity demand: Victoria (yearly pattern)"
                        :=x-title "Day of year"
                        :=y-title "MWh"}))

;; ## 2.5 — Seasonal subseries plots
;;
;; R: `gg_subseries(a10, Cost)` — for each month, show values across years
;; with the mean as a horizontal line.

;; Group by month, plot each month's values over years
(def a10-subseries
  (-> a10
      (time-api/add-time-columns "Month" {:year "Year" :month "MonthNum"})))

;; Faceted by month — each panel shows that month across all years.
;; Convert MonthNum to string so tableplot treats it as categorical.
(-> a10-subseries
    (tc/add-column "MonthLabel" #(mapv str (% "MonthNum")))
    (plotly/layer-line {:=x "Year"
                        :=y "Cost"
                        :=color "MonthLabel"
                        :=title "Seasonal subseries plot: Antidiabetic drug sales"
                        :=y-title "$ (millions)"}))

;; ## 2.6 — Scatterplots
;;
;; R: `ggplot(aes(x = Temperature, y = Demand)) + geom_point()`
;; Electricity demand vs temperature for 2014 Victoria.

(def vic-elec-2014
  (-> vic-elec
      (tc/select-rows #(= (.getYear (get % "Time")) 2014))))

(-> vic-elec-2014
    (plotly/layer-point {:=x "Temperature"
                         :=y "Demand"
                         :=title "Electricity demand vs temperature (Victoria, 2014)"
                         :=x-title "Temperature (°C)"
                         :=y-title "Demand (MWh)"}))

;; The U-shape: high demand for both cold (heating) and hot (air conditioning).
;; Correlation coefficient r = 0.28 — misleading for non-linear relationships.
;; Always plot first!

;; ### Scatterplot matrix — Australian tourism by state
(def tourism (load-fpp3 "tourism"))

(def visitors-by-state
  (-> tourism
      (tc/group-by ["Quarter" "State"])
      (tc/aggregate {"Trips" #(dfn/sum (% "Trips"))})
      ;; Pivot wider: one column per state
      (tc/pivot->wider "State" "Trips")))

visitors-by-state

;; For a scatterplot matrix, we'd plot each state column against every other.
;; Tableplot doesn't have a built-in pairs plot, but you can compose them.
;; Here's one pair as an example — NSW vs Victoria:

(-> visitors-by-state
    (plotly/layer-point {:=x "New South Wales"
                         :=y "Victoria"
                         :=title "Tourism: NSW vs Victoria (quarterly trips)"}))

;; ## 2.7 — Lag plots
;;
;; R: `gg_lag(Beer, geom = "point")`
;; Plot y_t against y_{t-k} for various lags.
;; This is the visual precursor to autocorrelation.

;; For beer production, lag 4 should show strong positive correlation (seasonal)
;; Using tablecloth.time.api/add-lags with auto-drop of missing values:
;; Note: add-lags creates keyword columns like :Beer_lag4
(-> recent-beer
    (time-api/add-lags "Beer" [4])
    (plotly/layer-point {:=x "Beer_lag4"
                         :=y "Beer"
                         :=title "Lag 4 plot: Australian beer production"
                         :=x-title "Beer (t-4)"
                         :=y-title "Beer (t)"}))

;; Strong positive diagonal = strong correlation at lag 4
;; (Q4 peaks align with Q4 peaks from the previous year)

;; ## 2.8 — Autocorrelation (ACF)
;;
;; r_k = Σ(y_t - ȳ)(y_{t-k} - ȳ) / Σ(y_t - ȳ)²
;;
;; This is a core function we need in tablecloth.time.
;; For now, let's compute it manually.

(defn acf
  "Compute autocorrelation coefficients for lags 1..max-lag.
  Returns a dataset with :lag and :acf columns."
  [values max-lag]
  (let [values (double-array (remove nil? values))
        n (alength values)
        mean (/ (areduce values i sum 0.0 (+ sum (aget values i))) n)
        ;; denominator: Σ(y_t - ȳ)²
        denom (areduce values i sum 0.0
                       (let [d (- (aget values i) mean)]
                         (+ sum (* d d))))
        lags (range 1 (inc max-lag))
        acf-vals (mapv (fn [k]
                         (let [numer (loop [t k, sum 0.0]
                                      (if (>= t n)
                                        sum
                                        (recur (inc t)
                                               (+ sum (* (- (aget values t) mean)
                                                        (- (aget values (- t k)) mean))))))]
                           (/ numer denom)))
                       lags)]
    (tc/dataset {"lag" (vec lags)
                 "acf" acf-vals})))

;; ### ACF of beer production
(def beer-acf (acf (recent-beer "Beer") 9))
beer-acf

;; Should match R output:
;; lag 1: -0.053, lag 2: -0.758, lag 4: 0.802, lag 8: 0.707

(let [T (count (remove nil? (vec (recent-beer "Beer"))))
      bound (/ 1.96 (Math/sqrt T))]
  (-> beer-acf
      (tc/add-column "upper" (repeat (tc/row-count beer-acf) bound))
      (tc/add-column "lower" (repeat (tc/row-count beer-acf) (- bound)))
      (plotly/layer-bar {:=x "lag"
                         :=y "acf"
                         :=title "ACF: Australian beer production"
                         :=y-title "Autocorrelation"})))

;; - r₄ is highest (seasonal: peaks 4 quarters apart)
;; - r₂ is most negative (peaks vs troughs, 2 quarters apart)
;; - Dashed lines at ±1.96/√T mark significance bounds

;; ### ACF of antidiabetic drug sales (trend + seasonality)
(def a10-acf (acf (a10 "Cost") 48))

(-> a10-acf
    (plotly/layer-bar {:=x "lag"
                       :=y "acf"
                       :=title "ACF: Australian antidiabetic drug sales"}))

;; Slow decay (trend) + scalloped shape (seasonality at lag 12, 24, 36...)

;; ## 2.9 — White noise
;;
;; A white noise series has no autocorrelation.
;; All ACF spikes should fall within ±1.96/√T.

(def white-noise
  (tc/dataset {"t" (range 1 51)
               "wn" (repeatedly 50 #(let [u1 (rand) u2 (rand)]
                                      (* (Math/sqrt (* -2 (Math/log u1)))
                                         (Math/cos (* 2 Math/PI u2)))))}))

(-> white-noise
    (plotly/layer-line {:=x "t"
                        :=y "wn"
                        :=title "White noise"}))

(def wn-acf (acf (white-noise "wn") 15))

(let [bound (/ 1.96 (Math/sqrt 50))]
  (-> wn-acf
      (tc/add-column "upper" (repeat (tc/row-count wn-acf) bound))
      (tc/add-column "lower" (repeat (tc/row-count wn-acf) (- bound)))
      (plotly/layer-bar {:=x "lag"
                         :=y "acf"
                         :=title "ACF: White noise"})))

;; All spikes should be within ±0.28 (= 1.96/√50)
;; → Confirms: no signal to model.

;; ## Appendix: Benchmarking seasonal plot approaches
;;
;; Two approaches to building seasonal plots with many traces:
;;
;; 1. **tableplot + kindly/f post-processing**: Let tableplot build traces,
;;    extract via `:kindly/f`, then post-process each trace
;; 2. **Pure manual traces**: Build Plotly traces directly with reduce
;;
;; Let's time them:

(defn seasonal-plot-manual
  "Build seasonal Plotly spec manually (no tableplot)."
  [ds phase-col value-col group-col color-fn
   & {:keys [line-width title x-title y-title]
      :or {line-width 0.3}}]
  (let [groups (-> ds (tc/group-by group-col) :data)
        traces (mapv (fn [group-ds]
                       (let [group-name (first (group-ds group-col))
                             sorted-ds (tc/order-by group-ds phase-col)]
                         {:x (vec (sorted-ds phase-col))
                          :y (vec (sorted-ds value-col))
                          :type "scatter"
                          :mode "lines"
                          :name group-name
                          :showlegend false
                          :line {:color (color-fn group-name)
                                 :width line-width}}))
                     groups)]
    {:data traces
     :layout {:title title
              :xaxis {:title x-title}
              :yaxis {:title y-title}}}))

;; ### Benchmark: tableplot+plot vs manual
;; Using the daily seasonal plot (700+ days = 700+ traces)

(let [color-fn #(get {"2011" "#7570b3" "2012" "#1b9e77" "2013" "#d95f02" "2014" "#7570b3"}
                     (subs % 0 4) "gray")
      n 10]
  {:tableplot+post-process
   (let [start (System/nanoTime)]
     (dotimes [_ n]
       (seasonal-plot-spec vic-elec-with-fields "DailyPhase" "Demand" "TimeDate" color-fn))
     (/ (- (System/nanoTime) start) 1e6 n))
   
   :manual-traces
   (let [start (System/nanoTime)]
     (dotimes [_ n]
       (seasonal-plot-manual vic-elec-with-fields "DailyPhase" "Demand" "TimeDate" color-fn))
     (/ (- (System/nanoTime) start) 1e6 n))})

;; ## Understanding kindly/f and plotly/plot
;;
;; Kindly is a portable notation protocol for Clojure visualizations.
;; When tableplot builds a plot, it returns a "recipe" map like:
;;
;; ```clojure
;; {:kindly/f #'plotly-xform        ;; transform function
;;  :data :=traces                  ;; placeholder
;;  ::ht/defaults {:=x "col" ...}}  ;; our bindings + dataset
;; ```
;;
;; The `:kindly/f` function transforms the recipe into actual Plotly JSON.
;; This defers evaluation — Clay/Portal call it when rendering.
;;
;; To get the raw spec for post-processing, use `plotly/plot`:
;; ```clojure
;; (let [viz (plotly/layer-line ...)
;;       spec (plotly/plot viz)]    ;; official API to force evaluation
;;   (update spec :data ...))       ;; now we can modify traces
;; ```
;;
;; Why deferred execution?
;; - Lazy composition (chain `layer-*` calls before computing)
;; - Tool flexibility (Clay, Portal render differently)
;; - Introspection (inspect the recipe without triggering evaluation)

;; ## Summary
;;
;; | R (fpp3)          | Clojure (tablecloth + tableplot)                    |
;; |-------------------|-----------------------------------------------------|
;; | `autoplot()`      | `plotly/layer-line`                                 |
;; | `gg_season()`     | extract year/month + `layer-line` with `:=color`    |
;; | `gg_subseries()`  | extract month + faceted/colored line plot            |
;; | `gg_lag()`        | manual lag column + `layer-point`                   |
;; | `ACF()`           | `acf` function (to be added to tablecloth.time)     |
;; | `ggplot + geom_*` | `plotly/layer-point`, `plotly/layer-bar`, etc.       |
;;
;; **New function for tablecloth.time:** `acf` — autocorrelation computation.
;; This should live in the column API alongside the field extractors.
