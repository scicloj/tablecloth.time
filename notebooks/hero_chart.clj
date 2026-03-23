(ns hero-chart
  (:require [tablecloth.api :as tc]
            [tablecloth.time.api :as tct]
            [scicloj.tableplot.v1.plotly :as plotly]
            [tech.v3.datatype.functional :as dfn]))

;; Load and prepare data
(def vic-elec
  (-> (tc/dataset "data/fpp3/vic_elec.csv")
      (tc/convert-types "Time" [:local-date-time "yyyy-MM-dd HH:mm:ss"])))

(def vic-elec-with-fields
  (-> vic-elec
      (tct/add-time-columns "Time"
                            {:week-of-year "WeekOfYear"
                             :year-string "YearStr"})))

;; Aggregate to weekly means
(def weekly-data
  (-> vic-elec-with-fields
      (tc/group-by ["YearStr" "WeekOfYear"])
      (tc/aggregate {"Demand" #(dfn/mean (% "Demand"))})))

;; Yearly seasonal chart - each line is a year
(-> weekly-data
    (tc/order-by ["YearStr" "WeekOfYear"])
    (plotly/layer-line {:=x "WeekOfYear"
                        :=y "Demand"
                        :=color "YearStr"
                        :=title "Yearly Electricity Demand Patterns"
                        :=x-title "Week of Year"
                        :=y-title "Demand (MW)"}))
