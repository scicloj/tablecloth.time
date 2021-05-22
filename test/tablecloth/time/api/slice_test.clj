(ns tablecloth.time.api.slice-test
  (:require [tablecloth.api :refer [dataset]]
            [tablecloth.time.utils.indexing-tools :refer [index-by]]
            [tablecloth.time.api :refer [slice]]
            [tech.v3.datatype.datetime :refer [long-temporal-field plus-temporal-amount]]
            [clojure.test :refer [deftest is are]]
            [time-literals.data-readers]))

(deftest slice-by-int
  (is (= (dataset {:A [2 3]
                   :B [5 6]})
         (-> (dataset {:A (long-temporal-field :day-of-year
                                               (plus-temporal-amount #time/date "1970-01-01" (range 3) :days))
                       :B [4 5 6]})
             (index-by :A)
             (slice 2 3)))))

(deftest slice-by-instant
  (are [_ arg-map] (= (dataset {:A [#time/instant "1970-01-01T09:00:00.000Z"
                                    #time/instant "1970-01-01T10:00:00.000Z"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/instant "1970-01-01T00:00:00.000Z" (range 11) :hours)
                                    :B (range 11)})

                          (slice (:from arg-map) (:to arg-map))))
    _ {:from "1970-01-01T09:00:00.000Z" :to "1970-01-01T10:00:00.000Z"}
    _ {:from #time/instant "1970-01-01T09:00:00.000Z" :to #time/instant "1970-01-01T10:00:00.000Z"}))

(deftest slice-by-local-datetime
  (are [_ arg-map] (= (dataset {:A [#time/date-time "1970-01-01T09:00"
                                    #time/date-time "1970-01-01T10:00"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/date-time "1970-01-01T00:00" (range 11) :hours)
                                    :B (range 11)})
                          (slice (:to arg-map) (:from arg-map))))
    _ {:to "1970-01-01T09:00" :from "1970-01-01T10:00:00"}
    _ {:to #time/date-time "1970-01-01T09:00" :from #time/date-time "1970-01-01T10:00"}))

(deftest slice-by-local-date
  (are [_ arg-map] (= (dataset {:A [#time/date "1979-01-01" #time/date "1980-01-01"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/date "1970-01-01" (range 11) :years)
                                    :B (range 11)})
                          (slice (:to arg-map) (:from arg-map))))
    _ {:to "1979-01-01" :from "1980-01-01"}
    _ {:to #time/date "1979-01-01" :from #time/date "1980-01-01"}))

(deftest slice-result-types
  (let [ds (dataset {:A [#time/date "1970-01-01"
                         #time/date "1970-01-02"
                         #time/date "1970-01-03"]
                     :B [4 5 6]})]
    (is (instance? tech.v3.datatype.ListPersistentVector
                   (-> ds
                       (slice "1970-01-02" "1970-01-03" {:result-type :as-indexes}))))
    (is (instance? tech.v3.dataset.impl.dataset.Dataset
                   (-> ds
                       (slice "1970-01-02" "1970-01-03" {:result-type :as-dataset}))))
    ;; default behavior
    (is (instance? tech.v3.dataset.impl.dataset.Dataset
                   (-> ds
                       (slice "1970-01-02" "1970-01-03"))))))
