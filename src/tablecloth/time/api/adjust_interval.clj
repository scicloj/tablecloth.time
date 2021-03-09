(ns tablecloth.time.api.adjust-interval
  (:import [org.threeten.extra YearQuarter YearWeek]
           [java.time LocalDate Year YearMonth])
  (:require [tech.v3.datatype :refer [emap]]
            [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.dataset :as tech-dataset]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api.conversion :refer [convert-to]]
            [tick.alpha.api :as tick]))

;; TODO Consider alternate syntax based on individual time converters, no multimethod
;; (adjust-interval [dataset index-col keys converter new-index-col-key])
;; A benefit of this would be ethat we do not need to decide the meaning of the keys
;; that we are using as the 'target-units'. Currently, they awkwardly overlap the
;; keys provided by tech.datetime and tick.

(defn adjust-interval
  "Change the time index frequency."
  ([dataset index-col-key keys target-unit]
   (adjust-interval dataset index-col-key keys target-unit nil))
  ([dataset index-col-key keys target-unit {:keys [ungroup?]
                                            :or {ungroup? false}}]
   (let [time-converter #(convert-to % target-unit)
         index-column  (index-col-key dataset)
         adjusted-column-data (emap time-converter target-unit index-column)]
     (-> dataset
         (tablecloth/add-or-replace-column target-unit adjusted-column-data)
         (tablecloth/group-by (into [target-unit] keys))
         ;; can remove when tech.dataset changes arggroup to return an ordered LinkedHashMap
         (tech-dataset/sort-by #(get-in % [:name target-unit]))
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

  (-> raw-ds
      (adjust-interval :instant [:symbol] :days)
      (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      )

  (-> raw-ds
      :instant
      (tablecloth/select-rows (range 5))
      (tech.v3.datatype/->reader)
      (->> (tech.v3.datatype.argops/arggroup-by identity {:unordered? false}))
      type
      )

  (tablecloth/select-rows )
  )
