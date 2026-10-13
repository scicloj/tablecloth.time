;; # Chapter 2: Time Series Graphics (Plotje version)
;; Translating fpp3 Chapter 2 examples from R to Clojure
;; using tablecloth, tablecloth.time, and plotje.
;;
;; Reference: https://otexts.com/fpp3/graphics.html
;; Baseline: chapter_02_time_series_graphics.clj (tableplot version)
;;
;; Review figures: `clj -M:notebooks -m plotje-review`

(ns chapter-02-plotje
  (:require [tablecloth.api :as tc]
            [tech.v3.datatype.functional :as dfn]
            [scicloj.plotje.api :as pj]
            [tablecloth.time.api :as tct]))

;; ## 2.1 — Loading data

(defn load-fpp3
  "Load one of the fpp3 datasets from CSV."
  [name]
  (tc/dataset (str "data/fpp3/" name ".csv") {:key-fn keyword}))

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
;; Timestamps are stored in UTC; convert them to Melbourne time so
;; years and seasons follow the local clock.
(def vic-elec
  (-> (load-fpp3 "vic_elec")
      (tc/convert-types :Time [:local-date-time "yyyy-MM-dd HH:mm:ss"])
      (tct/replace-time-zone :Time "UTC")
      (tct/convert-time-zone :Time "Australia/Melbourne")))
vic-elec

;; ### tourism — Australian quarterly overnight trips
(def tourism (load-fpp3 "tourism"))
tourism

;; ## 2.2 — Time plots

;; ### Ansett airlines: Melbourne-Sydney economy class
;; R: `autoplot(melsyd_economy, Passengers)`

(def melsyd-economy
  (-> ansett
      (tc/select-rows #(and (= "MEL-SYD" (:Airports %))
                            (= "Economy" (:Class %))))
      (tc/add-column :Passengers-000s #(dfn// (% :Passengers) 1000))))

(def melsyd-economy-plot
  (-> melsyd-economy
      (pj/lay-line :Week :Passengers-000s)
      (pj/options {:title "Ansett airlines economy class: Melbourne-Sydney"})))

melsyd-economy-plot

;; Notable features visible in the plot:
;; - 1989: No passengers (industrial dispute)
;; - 1992: Reduced load (economy seats replaced by business class trial)
;; - Late 1991: Large increase in passenger load
;; - Start of each year: Holiday-effect dips

;; ### Antidiabetic drug sales (a10)
;; R: `autoplot(a10, Cost)`

(def a10-plot
  (-> a10
      (pj/lay-line :Month :Cost)
      (pj/options {:title "Australian antidiabetic drug sales"
                   :y-label "$ (millions)"})))

a10-plot

;; Clear increasing trend with strong seasonality that grows proportionally.
;; The January spike each year is from stockpiling before year-end subsidies.

;; ## 2.3 — Time series patterns
;;
;; Three fundamental components:
;; - **Trend**: long-term increase/decrease
;; - **Seasonal**: fixed, known period (calendar-linked)
;; - **Cyclic**: rises/falls of variable, non-fixed frequency

;; ### Australian quarterly beer production (seasonal + no trend)
(def recent-beer
  (-> aus-production
      (tct/add-time-columns :Quarter {:year :Year})
      (tc/select-rows #(>= (:Year %) 2000))
      (tc/select-columns [:Quarter :Beer])))

(def recent-beer-plot
  (-> recent-beer
      (pj/lay-line :Quarter :Beer)
      (pj/options {:title "Australian quarterly beer production"})))

recent-beer-plot

;; ### Google stock — daily changes (no pattern, random walk)
(def gafa (load-fpp3 "gafa_stock"))

(def google-2015
  (-> gafa
      (tct/add-time-columns :Date {:year :Year})
      (tc/select-rows #(and (= "GOOG" (:Symbol %))
                            (= (:Year %) 2015)))))

;; Daily closing price
(def google-2015-close-plot
  (-> google-2015
      (pj/lay-line :Date :Close)
      (pj/options {:title "Google daily closing stock price (2015)"})))

google-2015-close-plot

;; Daily *change* in closing price; the first day has no previous close.
(def google-2015-change
  (-> google-2015
      (tct/add-lag :Close 1 :Close-lag1)
      (tc/drop-missing [:Close-lag1])
      (tc/add-column :Change #(dfn/- (% :Close) (% :Close-lag1)))
      (tc/select-columns [:Date :Change])))

(def google-2015-change-plot
  (-> google-2015-change
      (pj/lay-line :Date :Change)
      (pj/options {:title "Google daily change in closing stock price (2015)"})))

google-2015-change-plot

;; ## 2.6 — Scatterplots

;; ### Electricity demand and temperature in Victoria, 2014
(def vic-elec-2014
  (-> vic-elec
      (tct/add-time-columns :Time {:year :Year})
      (tc/select-rows #(= 2014 (:Year %)))
      (tc/add-column :Demand-GW #(dfn// (% :Demand) 1000))))

;; R: `autoplot(Demand)`
(def vic-elec-demand-plot
  (-> vic-elec-2014
      (pj/lay-line :Time :Demand-GW {:size 0.5})
      (pj/options {:title "Half-hourly electricity demand: Victoria"
                   :y-label "GW"})))

vic-elec-demand-plot

;; R: `autoplot(Temperature)`
(def vic-elec-temperature-plot
  (-> vic-elec-2014
      (pj/lay-line :Time :Temperature {:size 0.5})
      (pj/options {:title "Half-hourly temperatures: Melbourne, Australia"
                   :y-label "Degrees Celsius"})))

vic-elec-temperature-plot

;; R: `ggplot(aes(x = Temperature, y = Demand)) + geom_point()`
(def demand-vs-temperature-plot
  (-> vic-elec-2014
      (pj/lay-point :Temperature :Demand-GW {:size 1 :alpha 0.5})
      (pj/options {:title "Electricity demand vs temperature: Victoria, 2014"
                   :x-label "Temperature (degrees Celsius)"
                   :y-label "Electricity demand (GW)"})))

demand-vs-temperature-plot

;; Demand is high for both cold (heating) and hot (air conditioning) days.
;; A correlation coefficient misses this non-linear shape; plot first.

;; ### Scatterplot matrix — Australian tourism by state
;; R: `pivot_wider(values_from = Trips, names_from = State) |> GGally::ggpairs(columns = 2:9)`
(def visitors-by-state
  (-> tourism
      (tc/group-by [:Quarter :State])
      (tc/aggregate {:Trips #(dfn/sum (% :Trips))})
      (tc/pivot->wider :State :Trips)))

visitors-by-state

(def states (sort (remove #{:Quarter} (tc/column-names visitors-by-state))))

;; `pj/cross` pairs every state with every other; the diagonal shows each
;; state's distribution.
(def tourism-splom-plot
  (-> visitors-by-state
      (pj/pose (pj/cross states states) {:size 1.5})
      (pj/options {:width 1250
                   :height 1000})))

tourism-splom-plot

;; ## 2.7 — Lag plots
;; R: `recent_production |> gg_lag(Beer, geom = "point")`
;; Each panel plots beer production y_t against y_{t-k}, coloured by quarter.

(def lags (range 1 10))

(def beer-lags
  (-> recent-beer
      (tct/add-time-columns :Quarter {:quarter :Q})
      (tc/add-column :Quarter-of-year #(map (partial str "Q") (% :Q)))))

(defn lag-panel
  [k]
  (let [lagged (-> beer-lags
                   (tct/add-lag :Beer k :Beer-lagged)
                   (tc/drop-missing [:Beer-lagged])
                   (tc/order-by [:Q]))
        diagonal (tc/dataset {:from [(dfn/reduce-min (recent-beer :Beer))]
                              :to [(dfn/reduce-max (recent-beer :Beer))]})]
    (-> lagged
        (pj/lay-point :Beer-lagged :Beer {:color :Quarter-of-year})
        (pj/lay-segment :from :from {:data diagonal
                                     :x-end :to
                                     :y-end :to
                                     :stroke-dash :dashed
                                     :alpha 0.5
                                     :overlay true})
        (pj/options {:title (str "lag " k)
                     :x-label "lag(Beer, k)"
                     :color-label "Quarter"}))))

(def beer-lag-plot
  (pj/arrange (map lag-panel lags)
              {:cols 3
               :share-scales #{:x :y}
               :width 900
               :height 900}))

beer-lag-plot

;; Lags 4 and 8 sit on the diagonal: each quarter matches the same quarter
;; a year earlier. Lags 2 and 6 are negative: Q4 peaks pair with Q2 troughs.

;; Figures exported by `plotje-review`, keyed by output file name.
(def figures
  {"a10" a10-plot
   "melsyd-economy" melsyd-economy-plot
   "recent-beer" recent-beer-plot
   "google-2015-close" google-2015-close-plot
   "google-2015-change" google-2015-change-plot
   "vic-elec-demand-2014" vic-elec-demand-plot
   "vic-elec-temperature-2014" vic-elec-temperature-plot
   "demand-vs-temperature" demand-vs-temperature-plot
   "tourism-splom" tourism-splom-plot
   "beer-lag" beer-lag-plot})
