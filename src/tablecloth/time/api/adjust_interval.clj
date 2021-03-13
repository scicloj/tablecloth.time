(ns tablecloth.time.api.adjust-interval
  (:import [org.threeten.extra YearQuarter YearWeek]
           [java.time LocalDate Year YearMonth])
  (:require [tech.v3.datatype :refer [emap elemwise-datatype]]
            [tech.v3.datatype.datetime :as dtdt]
            [tech.v3.dataset :as tech-dataset]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api.conversion :as convert]))

;; TODO Consider alternate syntax based on individual time converters, no multimethod
;; (adjust-interval [dataset index-col keys converter new-index-col-key])
;; A benefit of this would be ethat we do not need to decide the meaning of the keys

;; keys provided by tech.datetime and tick.

tech.v3.datatype.casting/datatype->class-map
tech.v3.datatype.casting/class->datatype-map

(defn adjust-interval
  "Change the time index frequency."
  [dataset index-column-name keys time-converter new-column-name]
  (let [index-column (index-column-name dataset)
        target-datatype (-> index-column first time-converter elemwise-datatype)
        adjusted-column-data (emap time-converter target-datatype index-column)]
    (-> dataset
        (tablecloth/add-or-replace-column new-column-name adjusted-column-data)
        (tablecloth/group-by (into [new-column-name] keys)))))

(comment
  ;; (def raw-ds
  ;;   (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
  ;;       (tablecloth/dataset {:key-fn keyword})))

  (defn time-series [start-inst n tf]
    (dtdt/plus-temporal-amount start-inst (range n) tf))

  (defn get-test-ds [start-time num-rows temporal-unit]
    (tablecloth/dataset {:idx (time-series start-time num-rows temporal-unit)
                         :symbol "MSFT"
                         :price (take num-rows (repeatedly #(rand 200)))}))

  (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z"
                   (* 2 60)
                   :seconds)
      (adjust-interval :idx [:symbol] convert/->minutes :minutes)
      (tablecloth/ungroup)))
;; => nil
