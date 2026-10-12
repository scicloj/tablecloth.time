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
            [scicloj.plotje.api :as pj]))

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

;; ## 2.2 — Time plots

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

;; Figures exported by `plotje-review`, keyed by output file name.
(def figures
  {"a10" a10-plot})
