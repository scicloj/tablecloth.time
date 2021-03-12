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

  (def raw-ds (tablecloth/dataset {:instant (time-series
                                              #time/instant "1970-01-01T23:59:58.000Z"
                                              5000
                                              :seconds)
                                   :symbol "MSFT"
                                   :price (take 5000 (repeatedly #(rand 200)))}))

  (-> raw-ds )

  (-> raw-ds :instant last query-precision str clojure.string/lower-case keyword)

  (-> raw-ds
      (adjust-interval :instant [:symbol] convert/->every-5-seconds :every-5-seconds)
      (tablecloth/ungroup)
      ;; (tablecloth/aggregate {:price #(tech.v3.datatype.functional/mean (:price %))})
      )
;; => _unnamed [5000 4]:
;;    |             :instant | :symbol |       :price |     :every-5-seconds |
;;    |----------------------|---------|--------------|----------------------|
;;    | 1970-01-01T23:59:58Z |    MSFT |  62.95463766 | 1970-01-01T23:59:55Z |
;;    | 1970-01-01T23:59:59Z |    MSFT |  60.91447440 | 1970-01-01T23:59:55Z |
;;    | 1970-01-02T00:00:00Z |    MSFT |  92.01435884 | 1970-01-02T00:00:00Z |
;;    | 1970-01-02T00:00:01Z |    MSFT |   4.68111394 | 1970-01-02T00:00:00Z |
;;    | 1970-01-02T00:00:02Z |    MSFT |  22.48186792 | 1970-01-02T00:00:00Z |
;;    | 1970-01-02T00:00:03Z |    MSFT | 186.27562240 | 1970-01-02T00:00:00Z |
;;    | 1970-01-02T00:00:04Z |    MSFT |   9.67795926 | 1970-01-02T00:00:00Z |
;;    | 1970-01-02T00:00:05Z |    MSFT |  76.12415246 | 1970-01-02T00:00:05Z |
;;    | 1970-01-02T00:00:06Z |    MSFT |  56.84252176 | 1970-01-02T00:00:05Z |
;;    | 1970-01-02T00:00:07Z |    MSFT |  33.90504958 | 1970-01-02T00:00:05Z |
;;    | 1970-01-02T00:00:08Z |    MSFT |  19.95290404 | 1970-01-02T00:00:05Z |
;;    | 1970-01-02T00:00:09Z |    MSFT | 188.14010517 | 1970-01-02T00:00:05Z |
;;    | 1970-01-02T00:00:10Z |    MSFT | 178.84643388 | 1970-01-02T00:00:10Z |
;;    | 1970-01-02T00:00:11Z |    MSFT |  35.12785570 | 1970-01-02T00:00:10Z |
;;    | 1970-01-02T00:00:12Z |    MSFT | 123.83080837 | 1970-01-02T00:00:10Z |
;;    | 1970-01-02T00:00:13Z |    MSFT |  31.78611250 | 1970-01-02T00:00:10Z |
;;    | 1970-01-02T00:00:14Z |    MSFT | 179.04931083 | 1970-01-02T00:00:10Z |
;;    | 1970-01-02T00:00:15Z |    MSFT | 158.91854284 | 1970-01-02T00:00:15Z |
;;    | 1970-01-02T00:00:16Z |    MSFT |  74.71338150 | 1970-01-02T00:00:15Z |
;;    | 1970-01-02T00:00:17Z |    MSFT | 134.64968998 | 1970-01-02T00:00:15Z |
;;    | 1970-01-02T00:00:18Z |    MSFT |  86.99297160 | 1970-01-02T00:00:15Z |
;;    | 1970-01-02T00:00:19Z |    MSFT |  54.66690673 | 1970-01-02T00:00:15Z |
;;    | 1970-01-02T00:00:20Z |    MSFT |  87.06104330 | 1970-01-02T00:00:20Z |
;;    | 1970-01-02T00:00:21Z |    MSFT | 146.97409749 | 1970-01-02T00:00:20Z |
;;    | 1970-01-02T00:00:22Z |    MSFT | 132.84250853 | 1970-01-02T00:00:20Z |
  (-> raw-ds
      :instant
      (tablecloth/select-rows (range 5))
      (tech.v3.datatype/->reader)
      (->> (tech.v3.datatype.argops/arggroup-by identity {:unordered? false}))
      type
      )

  dtdt/milliseconds-in-second

  (-> (* 5 dtdt/milliseconds-in-second))

  [1 2 3 4 5 6 7 8 9 10]
  [0 2 4 8 10]

  [1 ;; 1 / 2 = .5
   2 ;; 2 / 2 = 1
   3 ;; 3 / 2 = 1.5
   4 ;; 4 / 2 = 2
   ]

  (def myinst (dtdt/instant))

  (let [myinst (dtdt/instant)
        ms (-> myinst convert/anytime->milliseconds)
        interval (* 5 dtdt/milliseconds-in-second)
        rem (mod ms interval)
        newms (- ms rem)
        new (-> newms (convert/milliseconds->anytime :instant))]
    [myinst ms new newms])

  )
;; => nil
