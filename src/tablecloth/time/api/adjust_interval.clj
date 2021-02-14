(ns tablecloth.time.api.adjust-interval
  (:import [org.threeten.extra YearQuarter YearWeek]
           [java.time YearMonth])
  (:require [tech.v3.datatype :refer [emap]]
            [tech.v3.datatype.datetime :as dtdt]
            [tablecloth.api :as tablecloth]
            [tick.alpha.api :as tick]))

;; D 	Calendar day ✅
;; W 	Weekly ✅
;; M 	Month end ✅
;; Q 	Quarter end
;; A 	Year end
;; H 	Hours
;; T 	Minutes
;; S 	Seconds
;; L 	Milliseonds
;; U 	Microseconds
;; N 	nanoseconds
;; B 	Business day
;; BM 	Business month end
;; BQ 	Business quarter end
;; BA 	Business year end
;; BH 	Business hours

(def map-time-unit->time-converter
  {:day tick/date
   :week (fn [datetime] (-> datetime YearWeek/from))
   :week-end (fn [datetime] (-> datetime
                              (YearWeek/from)
                              (.atDay java.time.DayOfWeek/SUNDAY)))
   :month-end (fn [datetime] (-> datetime
                                 (.with (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))
   :quarter (fn [datetime] (YearQuarter/from datetime))
   :year-month tick/year-month
   :year tick/year})

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

  ;; day - hmmm ends up sorted by day across years...
  (-> raw-ds
      (adjust-interval :date [:symbol] :day)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; year - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :year)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; quarter - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; week
  (-> raw-ds
      (adjust-interval :date [:symbol] :week)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; week-end - works?
  (-> raw-ds
      (adjust-interval :date [:symbol] :week-end)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; month-end
  (-> raw-ds
      (adjust-interval :date [:symbol] :month-end)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))}))

  ;; quarter start - ???
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter-start))

  ;; ungroup option - works
  (-> raw-ds
      (adjust-interval :date [:symbol] :quarter {:ungroup? true}))

  ;; let's say our finished functions, if they need to know about the index:
  ;; 1. know if they need an index
  ;; 2. create the index
  ;; 2. if not, can complain to the user.

  )

