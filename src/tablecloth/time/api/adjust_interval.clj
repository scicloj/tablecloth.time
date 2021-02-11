(ns tablecloth.time.api.adjust-interval
  (:require [tech.v3.datatype :refer [emap]]
            [tablecloth.api :as tablecloth]
            [tick.alpha.api :as tick]))

(def map-time-unit->time-converter
  {:year-month tick/year-month})

(defn adjust-interval
  "Change the time index frequency."
  [dataset index-col-key keys target-unit]
  (let [time-converter (target-unit map-time-unit->time-converter)
        index-column  (index-col-key dataset)
        adjusted-column-data (emap time-converter target-unit (index-col-key dataset))]
    (-> dataset
        (tablecloth/add-or-replace-column :year-month adjusted-column-data)
        (tablecloth/group-by (into [target-unit] keys)))))

(comment
  (def raw-ds
    (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
        (tablecloth/dataset {:key-fn keyword})))

  (def ds-msft
    (-> raw-ds
        (tablecloth/select-rows #(= "MSFT" (:symbol %)))))

  (tablecloth/head ds-msft)
;; => https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv [5 3]:
;;    | :symbol |      :date | :price |
;;    |---------|------------|--------|
;;    |    MSFT | 2000-01-01 |  39.81 |
;;    |    MSFT | 2000-02-01 |  36.35 |
;;    |    MSFT | 2000-03-01 |  43.22 |
;;    |    MSFT | 2000-04-01 |  28.37 |
;;    |    MSFT | 2000-05-01 |  25.45 |(.get #time/date "2020-12-10" java.time.temporal.IsoFields/QUARTER_YEAR)

  ;; doing some things more manually
  (-> ds-msft
      (tablecloth/add-or-replace-column :year-month (fn [ds] (map tick/year-month (:date ds))))
      (tablecloth/group-by [:year-month :symbol]))


  ;; what if the index column is less granular? will that ever happen? I don't think that makes sense. Can't image a use-case.

  (->> (tech.v3.dataset.readers/mapseq-reader ds-msft)
       (tech.v3.datatype.argops/arggroup-by #(-> % :date tick.alpha.api/year-month)))

  (-> ds-msft
      (tech.v3.dataset/select-columns [:symbol :date])
      (tech.v3.dataset/group-by->indexes identity)

      )



)
