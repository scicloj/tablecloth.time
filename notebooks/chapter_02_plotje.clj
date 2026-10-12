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

;; Figures exported by `plotje-review`, keyed by output file name.
(def figures
  {"a10" a10-plot
   "melsyd-economy" melsyd-economy-plot
   "recent-beer" recent-beer-plot})
