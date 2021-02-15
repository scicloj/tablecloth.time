(ns tablecloth.time.api.adjust-interval
  (:import [org.threeten.extra YearQuarter YearWeek]
           [java.time LocalDate Year YearMonth])
  (:require [tech.v3.datatype :refer [emap]]
            [tech.v3.datatype.datetime :as dtdt]
            [tablecloth.api :as tablecloth]
            [tick.alpha.api :as tick]))

;; D 	Calendar day ✅
;; W 	Weekly ✅
;; M 	Month end ✅
;; Q 	Quarter end ✅
;; A 	Year end
;; H 	Hours
;; T 	Minutes
;; S 	Seconds ✅
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
   :week (fn [datetime] (-> datetime
                            (YearWeek/from)
                            (.atDay java.time.DayOfWeek/SUNDAY)))
   :month (fn [datetime] (-> datetime
                             (.with (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))
   :quarter (fn [datetime]
              (.atEndOfQuarter (YearQuarter/from datetime)))
   :year (fn [datetime]
               (-> datetime tick/date (.with (java.time.temporal.TemporalAdjusters/lastDayOfYear))))
   :seconds (fn [datetime]
              (-> datetime
                  (dtdt/plus-temporal-amount 1 :seconds)
                  (.truncatedTo java.time.temporal.ChronoUnit/SECONDS)))})

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
  ;; (def raw-ds
  ;;   (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
  ;;       (tablecloth/dataset {:key-fn keyword})))

  (defn time-series [start-inst n tf]
    (dtdt/plus-temporal-amount start-inst (range n) tf))


  (def raw-ds (tablecloth/dataset {:instant (time-series
                                              #time/instant "1970-01-01T23:59:58.000Z"
                                              5000
                                              :milliseconds)
                                   :symbol "MSFT"
                                   :price (take 5000 (repeatedly #(rand 200)))}))

  (-> raw-ds :instant last)
  ;; => #time/instant "1970-01-02T00:00:02.999Z"


  (tick/date #time/instant "1970-01-02T08:00:02.999Z")
  (.truncateTo #time/instant "1970-01-02T08:00:02.999Z" java.time.temporal.ChronoUnit/DAYS)

  (.with #time/instant "1970-01-02T00:00:02.999Z" java.time.temporal.TemporalAdjusters/)

  ;; day
  (-> raw-ds
      (adjust-interval :instant [:symbol] :day)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      )

  ;; seconds
  (-> raw-ds
      (adjust-interval :instant [:symbol] :seconds)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      (tech.v3.dataset/sort-by-column :seconds))

  ;; let's say our finished functions, if they need to know about the index:
  ;; 1. know if they need an index
  ;; 2. create the index
  ;; 2. if not, can complain to the user.

  )

