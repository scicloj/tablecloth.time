(ns tablecloth.time.operations-test
  (:require [tablecloth.api :refer [dataset columns column-names]]
            [tablecloth.time.index :refer [index-by]]
            [tablecloth.time.operations :as ops]
            [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [clojure.test :refer [deftest is are]]))

;; TODO Consider switch tests to use midje: https://github.com/marick/Midje

;; Temporary until = fixed for datasets in tech.ml
(defn ds-equal? [dsa dsb]
  (let [colnames-equal (= (column-names dsa)
                          (column-names dsb))
        cols-equal (every?
                    #(= (first %) (second %))
                    (partition 2 (interleave (columns dsa) (columns dsb))))]
    (and colnames-equal cols-equal)))

(deftest slice-by-instant
  (are [_ arg-map] (= (dataset {:A [#time/instant "1970-01-01T09:00:00.000Z"
                                    #time/instant "1970-01-01T10:00:00.000Z"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/instant "1970-01-01T00:00:00.000Z" (range 11) :hours)
                                    :B (range 11)})
                          (index-by :A)
                          (ops/slice (:to arg-map) (:from arg-map))))
    _ {:to "1970-01-01T09:00:00.000Z" :from "1970-01-01T10:00:00.000Z"}
    _ {:to #time/instant "1970-01-01T09:00:00.000Z" :from #time/instant "1970-01-01T10:00:00.000Z"}))

(deftest slice-by-local-datetime
  (are [_ arg-map] (= (dataset {:A [#time/date-time "1970-01-01T09:00"
                                    #time/date-time "1970-01-01T10:00"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/date-time "1900-01-01T00:00" (range 11) :hours)
                                    :B (range 11)})
                          (index-by :A)
                          (ops/slice (:to arg-map) (:from arg-map))))
    _ {:to "1970-01-01T09:00" :from "1970-01-01T10:00:00"}
    _ {:to #time/date-time "1970-01-01T09:00" :from #time/date-time "1970-01-01T10:00"}))

(deftest slice-by-year
  (are [_ arg-map] (= (dataset {:A [#time/year "1979" #time/year "1980"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/year "1970" (range 11) :years)
                                    :B (range 11)})
                          (index-by :A)
                          (ops/slice (:to arg-map) (:from arg-map))))
    _ {:to "1979" :from "1980"}
    _ {:to #time/year "1979" :from #time/year "1980"}))

(deftest slice-by-year-month
  (are [_ arg-map] (= (dataset {:A [#time/year-month "1979-01" #time/year-month "1980-01"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/year-month "1970-01" (range 11) :years)
                                    :B (range 11)})
                          (index-by :A)
                          (ops/slice (:to arg-map) (:from arg-map))))
    _ {:to "1979-01" :from "1980-01"}
    _ {:to #time/year-month "1979-01" :from #time/year-month "1980-01"}))

(deftest slice-by-local-date
  (are [_ arg-map] (= (dataset {:A [#time/date "1970-01-09" #time/date "1970-01-10"]
                                :B [9 10]})
                      (-> (dataset {:A (plus-temporal-amount #time/date "1970-01-01" (range 11) :years)
                                    :B (range 11)})
                          (index-by :A)
                          (ops/slice (:to arg-map) (:from arg-map))))
    _ {:to "1979-01-01" :from "1980-01-01"}
    _ {:to #time/date "1979-01-01" :from #time/date "1980-01-01"}))

