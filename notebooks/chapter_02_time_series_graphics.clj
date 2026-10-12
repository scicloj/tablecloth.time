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
            [tablecloth.time.api :as tct])
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
  (tc/dataset (str "data/fpp3/" name ".csv") {:key-fn keyword}))

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
      (tc/select-rows #(= "A10" (:ATC2 %)))
      (tc/select-columns [:Month :Concession :Type :Cost])
      (tc/group-by [:Month])
      (tc/aggregate {:TotalC #(dfn/sum (% :Cost))})
      (tc/add-column :Cost #(dfn// (% :TotalC) 1e6))))

a10

;; ### ansett — Ansett airlines weekly passenger data
(def ansett (load-fpp3 "ansett"))
ansett

;; ### aus_production — Australian quarterly production
(def aus-production (load-fpp3 "aus_production"))
aus-production

;; ### vic_elec — Victorian half-hourly electricity demand
;;
;; **Timezone handling:** The CSV stores timestamps in UTC (e.g., "2011-12-31 13:00:00"),
;; but they represent Melbourne local events. We need to convert to Melbourne time
;; so that seasonal patterns (daily/weekly) align with local clock time.
;;
;; The conversion is two steps:
;; 1. `replace-time-zone "UTC"` — stamps UTC onto the naive LocalDateTime
;;    (13:00 becomes 13:00 UTC, no value change)
;; 2. `convert-time-zone "Australia/Melbourne"` — shifts to Melbourne local time
;;    (13:00 UTC → 00:00 Melbourne next day, since Melbourne is UTC+11 in summer)
;;
;; After this, `Time` is a ZonedDateTime in Melbourne timezone, and hour extraction
;; gives local hours (0 = midnight Melbourne, not 13 = UTC).
(def vic-elec
  (-> (load-fpp3 "vic_elec")
      (tc/convert-types :Time [:local-date-time "yyyy-MM-dd HH:mm:ss"])
      (tct/replace-time-zone :Time "UTC")
      (tct/convert-time-zone :Time "Australia/Melbourne")))
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
      (tc/select-rows #(and (= "MEL-SYD" (:Airports %))
                            (= "Economy" (:Class %))))
      (tc/add-column :Passengers-000s #(dfn// (% :Passengers) 1000))))

(-> melsyd-economy
    (plotly/layer-line {:=x :Week
                        :=y :Passengers-000s
                        :=title "Ansett airlines economy class: Melbourne-Sydney"}))

;; Notable features visible in the plot:
;; - 1989: No passengers (industrial dispute)
;; - 1992: Reduced load (economy seats replaced by business class trial)
;; - Late 1991: Large increase in passenger load
;; - Start of each year: Holiday-effect dips

;; ### Antidiabetic drug sales (a10)
(-> a10
    (plotly/layer-line {:=x :Month
                        :=y :Cost
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
      (tct/add-time-columns :Quarter {:year :Year})
      (tc/select-rows #(>= (:Year %) 2000))
      (tc/select-columns [:Quarter :Beer])))

(-> recent-beer
    (plotly/layer-line {:=x :Quarter
                        :=y :Beer
                        :=title "Australian quarterly beer production"}))

;; ### Google stock — daily changes (no pattern, random walk)
(def gafa (load-fpp3 "gafa_stock"))

(def google-2015
  (-> gafa
      (tct/add-time-columns :Date {:year :Year})
      (tc/select-rows #(and (= "GOOG" (:Symbol %))
                            (= (:Year %) 2015)))))

;; Daily closing price
(-> google-2015
    (plotly/layer-line {:=x :Date
                        :=y :Close
                        :=title "Google daily closing stock price (2015)"}))

;; Daily *change* in closing price
(let [closes (vec (google-2015 :Close))
      diffs (mapv - (rest closes) (butlast closes))]
  (-> (tc/dataset {:Date (rest (vec (google-2015 :Date)))
                   :Change diffs})
      (plotly/layer-line {:=x :Date
                          :=y :Change
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
      (tct/add-time-columns :Month {:year :Year :month :MonthNum})))

;; Year is int64 — tableplot treats numeric columns as continuous color scales.
;; Convert to string so it's treated as categorical (one line per year).
(-> a10-seasonal
    (tc/add-column :YearStr #(mapv str (% :Year)))
    (plotly/layer-line {:=x :MonthNum
                        :=y :Cost
                        :=color :YearStr
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
      (tct/add-time-columns :Time
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
                        :=y :Demand
                        :=color :YearStr
                        :=title "Electricity demand: Victoria (yearly pattern)"
                        :=x-title "Day of year"
                        :=y-title "MWh"}))

;; ## 2.5 — Seasonal subseries plots
;;
;; R: `gg_subseries(a10, Cost)` — for each month, show values across years
;; with the mean as a horizontal line.
;;
;; Plotly doesn't have declarative faceting like ggplot2, so we:
;; 1. Group by month
;; 2. Create a trace per month, each assigned to its own axis
;; 3. Manually set axis domains to create the subplot grid

(defn make-subseries-traces
  "Generate Plotly traces for a subseries plot.
   Each group becomes a data trace + mean line, assigned to its own axis."
  [grouped-data x-col y-col]
  (->> grouped-data
       (map-indexed
        (fn [idx group-ds]
          (let [axis-num (inc idx)  ; 1-indexed for Plotly
                xaxis (if (= axis-num 1) "x" (str "x" axis-num))
                yaxis (if (= axis-num 1) "y" (str "y" axis-num))
                xs (vec (group-ds x-col))
                ys (vec (group-ds y-col))
                mean-y (tcc/mean (group-ds y-col))]
            [{:x xs
              :y ys
              :type "scatter"
              :mode "lines+markers"
              :marker {:size 4}
              :line {:width 1}
              :xaxis xaxis
              :yaxis yaxis
              :name (str "Month " axis-num)
              :showlegend false}
             {:x [(first xs) (last xs)]
              :y [mean-y mean-y]
              :type "scatter"
              :mode "lines"
              :line {:color "blue" :width 2}
              :xaxis xaxis
              :yaxis yaxis
              :showlegend false}])))
       (apply concat)  ; flatten the pairs
       vec))

(def month-names ["Jan" "Feb" "Mar" "Apr" "May" "Jun"
                  "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"])

(defn make-subseries-layout
  "Generate layout with axis domains for n subplots in a row.
   Options:
   - :y-range [min max] - shared y-axis range across all subplots
   - :y-title - y-axis label (shown on first subplot only)"
  [n title & {:keys [y-range y-title] :or {y-title "$ (millions)"}}]
  (let [width (/ 0.92 n)
        gap 0.005
        month-annotations
        (mapv (fn [idx]
                (let [start (+ (* idx (+ width gap)) 0.04)
                      center (+ start (/ width 2))]
                  {:x center
                   :y 1.02
                   :xref "paper"
                   :yref "paper"
                   :text (get month-names idx (str "M" (inc idx)))
                   :showarrow false
                   :font {:size 10}}))
              (range n))]
    (reduce
     (fn [layout idx]
       (let [axis-num (inc idx)
             x-key (if (= axis-num 1) :xaxis (keyword (str "xaxis" axis-num)))
             y-key (if (= axis-num 1) :yaxis (keyword (str "yaxis" axis-num)))
             x-anchor (if (= axis-num 1) "y" (str "y" axis-num))
             y-anchor (if (= axis-num 1) "x" (str "x" axis-num))
             start (+ (* idx (+ width gap)) 0.04)
             end (+ start width)
             first-subplot? (= idx 0)
             y-axis-config (cond-> {:domain [0.15 0.95]
                                    :anchor y-anchor
                                    :tickfont {:size 8}}
                             first-subplot? (assoc :title y-title)
                             (not first-subplot?) (assoc :showticklabels false)
                             y-range (assoc :range y-range))]
         (-> layout
             (assoc x-key {:domain [start end]
                           :anchor x-anchor
                           :tickangle 45
                           :tickfont {:size 7}
                           :dtick 5})
             (assoc y-key y-axis-config))))
     {:title title
      :showlegend false
      :height 350
      :width 1200
      :annotations (conj month-annotations
                         {:x 0.5 :y -0.08
                          :xref "paper" :yref "paper"
                          :text "Month"
                          :showarrow false
                          :font {:size 12}})}
     (range n))))

;; Build the subseries plot
(def a10-with-fields
  (-> a10
      (tct/add-time-columns :Month {:year :Year :month :MonthNum})))

(def a10-grouped
  (-> a10-with-fields
      (tc/order-by [:MonthNum :Year])
      (tc/group-by [:MonthNum])
      :data))

;; Calculate shared y-axis range across all months (with 5% padding)
(def a10-y-range
  (let [all-costs (a10-with-fields :Cost)
        y-min (dfn/reduce-min all-costs)
        y-max (dfn/reduce-max all-costs)
        padding (* 0.05 (- y-max y-min))]
    [(- y-min padding) (+ y-max padding)]))

(def a10-subseries-traces (make-subseries-traces a10-grouped :Year :Cost))
(def a10-subseries-layout (make-subseries-layout 12 "Subseries: Australian antidiabetic drug sales"
                                                 :y-range a10-y-range))

(kind/plotly
 {:data a10-subseries-traces
  :layout a10-subseries-layout})



;; ## Example: Australian Tourism (Figure 2.9)
;; 
;; Quarterly overnight trips by State, filtered to Holiday purpose.
;; Uses tc/sum as aggregator directly on grouped dataset.

(def tourism (load-fpp3 "tourism"))

(-> tourism
    (tc/select-rows #(= (:Purpose %) "Holiday"))
    (tc/group-by [:Quarter :State])
    (tc/sum [:Trips])
    (tc/rename-columns {:summary :Trips})
    (tc/order-by :Quarter)
    (plotly/layer-line {:=x :Quarter
                          :=y "summary"
                          :=color :State
                          :=title "Australian domestic holidays"
                          :=y-title "Overnight trips ('000)"}))

;; ## Figure 2.10 — Vertical Faceted Plot (Trips by State, colored by Region)
;;
;; Plotly doesn't have declarative faceting, so we build it manually:
;; - Vertical stack of subplots (one per State)
;; - Multiple lines per subplot (one per Region, colored)
;; - Shared x-axis, independent y-axes
;; - Facet labels on the right

(defn make-vertical-facet-traces
  "Generate Plotly traces for a vertically-faceted line plot.
   
   Arguments:
   - ds: dataset with columns for x, y, facet, and color
   - facet-col: column to facet by (e.g. \"State\") - creates one subplot per value
   - x-col: x-axis column (e.g. \"Quarter\")
   - y-col: y-axis column (e.g. \"Trips\")
   - color-col: column to color lines by (e.g. \"Region\")
   
   Returns vector of Plotly trace maps, each assigned to appropriate axes."
  [ds facet-col x-col y-col color-col]
  (let [facet-values (sort (distinct (ds facet-col)))
        color-values (sort (distinct (ds color-col)))
        ;; Simple color palette
        colors ["#1f77b4" "#ff7f0e" "#2ca02c" "#d62728" "#9467bd"
                "#8c564b" "#e377c2" "#7f7f7f" "#bcbd22" "#17becf"
                "#aec7e8" "#ffbb78" "#98df8a" "#ff9896" "#c5b0d5"
                "#c49c94" "#f7b6d2" "#c7c7c7" "#dbdb8d" "#9edae5"]
        color-map (zipmap color-values (cycle colors))]
    (->> (for [[facet-idx facet-val] (map-indexed vector facet-values)
               color-val color-values]
           (let [axis-num (inc facet-idx)
                 yaxis (if (= axis-num 1) "y" (str "y" axis-num))
                 subset (-> ds
                            (tc/select-rows #(and (= (get % facet-col) facet-val)
                                                  (= (get % color-col) color-val)))
                            (tc/order-by x-col))
                 xs (vec (subset x-col))
                 ys (vec (subset y-col))]
             (when (seq xs)
               {:x xs
                :y ys
                :type "scatter"
                :mode "lines"
                :line {:width 1 :color (color-map color-val)}
                :yaxis yaxis
                :name color-val
                :legendgroup color-val
                :showlegend (= facet-idx 0)})))  ; only show legend for first facet
         (remove nil?)
         vec)))

(defn make-vertical-facet-layout
  "Generate layout for vertically-stacked subplots.
   
   Arguments:
   - facet-names: seq of facet values (e.g. [\"ACT\" \"NSW\" ...])
   - title: plot title
   
   Options:
   - :height - total plot height (default 800)
   - :width - plot width (default 900)
   - :x-title - x-axis label
   - :y-title - y-axis label (shown on middle subplot)"
  [facet-names title & {:keys [height width x-title y-title]
                        :or {height 800 width 900}}]
  (let [n (count facet-names)
        subplot-height (/ 0.85 n)
        gap 0.02
        ;; Facet label annotations (on the right)
        facet-annotations
        (mapv (fn [idx facet-name]
                (let [top (- 0.95 (* idx (+ subplot-height gap)))
                      center (- top (/ subplot-height 2))]
                  {:x 1.02
                   :y center
                   :xref "paper"
                   :yref "paper"
                   :text facet-name
                   :showarrow false
                   :font {:size 10}
                   :textangle 0}))
              (range n)
              facet-names)]
    (reduce
     (fn [layout idx]
       (let [axis-num (inc idx)
             y-key (if (= axis-num 1) :yaxis (keyword (str "yaxis" axis-num)))
             top (- 0.95 (* idx (+ subplot-height gap)))
             bottom (- top subplot-height)
             middle-subplot? (= idx (quot n 2))]
         (assoc layout y-key
                (cond-> {:domain [bottom top]
                         :anchor "x"
                         :tickfont {:size 8}}
                  middle-subplot? (assoc :title (or y-title ""))
                  (not middle-subplot?) (assoc :title "")))))
     {:title title
      :showlegend true
      :legend {:x 1.08 :y 0.5 :font {:size 8}}
      :height height
      :width width
      :margin {:r 150}  ; room for facet labels and legend
      :xaxis {:domain [0.05 0.95]
              :title (or x-title "")
              :tickfont {:size 9}}
      :annotations facet-annotations}
     (range n))))

;; Build Figure 2.10: Holiday trips faceted by State, colored by Region
(def tourism-holiday
  (-> tourism
      (tct/add-time-columns :Quarter {:year :Year
                                      :quarter :QuarterNum})
      (tc/select-rows #(= (:Purpose %) "Holiday"))
      (tc/order-by :Year)
      ))

(tc/head tourism-holiday)

   ;; - facet-col: column to facet by (e.g. \"State\") - creates one subplot per value
   ;; - x-col: x-axis column (e.g. \"Quarter\")
   ;; - y-col: y-axis column (e.g. \"Trips\")
   ;; - color-col: column to color lines by (e.g. \"Region\")
(def fig-2-10-traces
  (make-vertical-facet-traces
   tourism-holiday
   :State
   :QuarterNum
   :Trips
   :Year))

(def fig-2-10-layout
  (make-vertical-facet-layout
   (sort (distinct (tourism-holiday :State)))
   "Australian domestic tourism: Holiday trips"
   :x-title "Quarter"
   :y-title "Trips ('000)"
   :height 900))

(kind/plotly
 {:data fig-2-10-traces
  :layout fig-2-10-layout})

;; ## 2.6 — Scatterplots
;;
;; R: `ggplot(aes(x = Temperature, y = Demand)) + geom_point()`
;; Electricity demand vs temperature for 2014 Victoria.

(def vic-elec-2014
  (-> vic-elec
      (tct/add-time-columns :Time {:year :Year})
      (tc/select-rows #(= (:Year %) 2014))))

(-> vic-elec-2014
    (tct/slice :Time "2014-01-01" "2014-12-31")
    (plotly/base {:=width 600})
    (plotly/layer-line {:=x :Time
                        :=y :Demand
                        :=title "Half-hour electricity demand: Victoria"}))

(-> vic-elec-2014
    (tct/slice :Time "2014-01-01" "2014-12-31")
    (plotly/base {:=width 600})
    (plotly/layer-line {:=x :Time
                        :=y :Temperature
                        :=title "Half-hourly temperatures: Melboure, Australia"}))

(-> vic-elec-2014
    (plotly/layer-point {:=x :Temperature
                         :=y :Demand
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
      (tc/group-by [:Quarter :State])
      (tc/aggregate {:Trips #(dfn/sum (% :Trips))})
      ;; Pivot wider: one column per state
      (tc/pivot->wider :State :Trips)))

visitors-by-state

;; For a scatterplot matrix, we'd plot each state column against every other.
;; Tableplot doesn't have a built-in pairs plot, but you can compose them.
;; Here's one pair as an example — NSW vs Victoria:

(-> visitors-by-state
    (plotly/layer-point {:=x :New-South-Wales
                         :=y :Victoria
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
    (tct/add-lags :Beer [4])
    (plotly/layer-point {:=x :Beer_lag4
                         :=y :Beer
                         :=title "Lag 4 plot: Australian beer production"
                         :=x-title "Beer (t-4)"
                         :=y-title "Beer (t)"}))

;; Strong positive diagonal = strong correlation at lag 4
;; (Q4 peaks align with Q4 peaks from the previous year)

;; ## 2.8 — Autocorrelation (ACF)
;;
;; Temporarily removed while migrating the notebook to keywordized CSV headers.
;; We can restore this section once the broader key-fn cleanup settles.

;; ## 2.9 — White noise
;;
;; White noise + ACF example temporarily removed along with the ACF section above.

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
