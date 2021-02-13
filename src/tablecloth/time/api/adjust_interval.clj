(ns tablecloth.time.api.adjust-interval
  (:import org.threeten.extra.YearQuarter)
  (:require [tech.v3.datatype :refer [emap]]
            [tablecloth.api :as tablecloth]
            [tick.alpha.api :as tick]))

(def map-time-unit->time-converter
  {:quarter (fn [datetime] (YearQuarter/from datetime))
   :year-month tick/year-month
   :year tick/year
   :instant tick/instant})

(defn adjust-interval
  "Change the time index frequency."
  ([dataset index-col-key keys target-unit]
   (adjust-interval dataset index-col-key keys target-unit nil))
  ([dataset index-col-key keys target-unit {:keys [ungroup?]
                                            :or {ungroup? false}}]
   (let [time-converter (target-unit map-time-unit->time-converter)
         index-column  (index-col-key dataset)
         adjusted-column-data (emap time-converter target-unit (index-col-key dataset))]
     (-> dataset
         (tablecloth/add-or-replace-column target-unit adjusted-column-data)
         (tablecloth/group-by (into [target-unit] keys))
         (cond-> ungroup? tablecloth/ungroup
                 (not ungroup?) identity)))))

(comment
  (def raw-ds
    (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
        (tablecloth/dataset {:key-fn keyword})))

  raw-ds

  (def ds-msft
    (-> raw-ds
        (tablecloth/select-rows #(= "MSFT" (:symbol %)))))

  (tablecloth/head ds-msft)


  ;; year - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :year)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; quarter - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; quarter start - ???
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter-start))

  ;; adjust to smaller interval - ??
  (-> raw-ds
      (adjust-interval :date [:symbol] :instant))

  ;; ungroup option - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter {:ungroup? true}))
  )
