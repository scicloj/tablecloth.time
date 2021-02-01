(ns tablecloth.time.operations-test
  (:require [tablecloth.api :refer [dataset columns column-names]]
            [tablecloth.time.index :refer [index-by]]
            [tablecloth.time.operations :as ops]
            [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tick.alpha.api :as t]
            [clojure.test :refer [deftest is]]))

;; TODO Consider switch tests to use midje: https://github.com/marick/Midje

;; Temporary until = fixed for datasets in tech.ml
(defn ds-equal? [dsa dsb]
  (let [colnames-equal (= (column-names dsa)
                          (column-names dsb))
        cols-equal (every?
                    #(= (first %) (second %))
                    (partition 2 (interleave (columns dsa) (columns dsb))))]
    (and colnames-equal cols-equal)))

(deftest slice-by-date
  (is (ds-equal? (dataset {:A [(t/date "1970-01-09") (t/date "1970-01-10")]
                           :B [8 9]})
                 (-> (dataset {:A (plus-temporal-amount (t/date "1970-01-01") (range 10) :days)
                               :B (range 10)})
                     (index-by :A)
                     (ops/slice-by-date "1970-01-09" "1970-01-10")))))

(deftest slice-by-year
  (is (ds-equal? (dataset {:A [(t/year "1979") (t/year "1980")]
                           :B [9 10]})
                 (-> (dataset {:A (plus-temporal-amount (t/year 1970) (range 11) :years)
                               :B (range 11)})
                     (index-by :A)
                     (ops/slice-by-year "1979" "1980")))))

(deftest slice-by-datetime
  (is (ds-equal? (dataset {:A [(t/date-time "1970-01-01T09:00") (t/date-time "1970-01-01T10:00")]
                           :B [9 10]})
                 (-> (dataset {:A (plus-temporal-amount (t/date-time "1970-01-01T00:00") (range 11) :hours)
                               :B (range 11)})
                     (index-by :A)
                     (ops/slice-by-datetime "1970-01-01T09:00" "1979-01-01T10:00")))))
