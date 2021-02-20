(ns tablecloth.time.api.adjust-interval
  (:import [org.threeten.extra YearQuarter YearWeek]
           [java.time LocalDate Year YearMonth])
  (:require [tech.v3.datatype :refer [emap]]
            [tech.v3.datatype.datetime :as dtdt]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api :refer [truncate-to]]
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

;; (def map-time-unit->time-converter
;;   {:day tick/date
;;    :month (fn [datetime] (-> datetime
;;                              (.with (java.time.temporal.TemporalAdjusters/lastDayOfMonth))))
;;    :quarter (fn [datetime]
;;               (.atEndOfQuarter (YearQuarter/from datetime)))
;;    :year (fn [datetime]
;;                (-> datetime tick/date (.with (java.time.temporal.TemporalAdjusters/lastDayOfYear))))
;;   })

(defn adjust-interval
  "Change the time index frequency."
  ([dataset index-col-key keys target-unit]
   (adjust-interval dataset index-col-key keys target-unit nil))
  ([dataset index-col-key keys target-unit {:keys [ungroup?]
                                            :or {ungroup? false}}]
   (let [time-converter #(truncate-to % target-unit)
         index-column  (index-col-key dataset)
         adjusted-column-data (emap time-converter target-unit index-column)]
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

  ;; day
  (-> raw-ds
      (adjust-interval :instant [:symbol] :day)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      )

  ;; seconds
  (-> raw-ds
      (adjust-interval :instant [:symbol] :days)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      )
  
)
