(ns scratch-holidays
  (:require [tablecloth.api :as tc]
            [tablecloth.time.api :as tct]
            [tablecloth.time.column.api :as tct-col]
            [scicloj.tableplot.v1.plotly :as plotly]))

(def tourism (tc/dataset "data/fpp3/tourism.csv"))

(def holidays
  (-> tourism
      (tc/select-rows #(= (get % "Purpose") "Holiday"))
      (tc/group-by ["State" "Quarter"])
      (tc/aggregate {"Trips" #(reduce + (% "Trips"))})
      (tc/add-column "Quarter" #(tct-col/convert-time (% "Quarter") :local-date))
      (tct/add-time-columns "Quarter" [:year :month])
      (tc/add-column "Q" #(mapv (fn [m] (str "Q" (inc (quot (dec m) 3)))) (% :month)))))

(tc/head holidays 20)
