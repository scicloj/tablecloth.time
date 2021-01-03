(ns tablecloth.time.operations-test
  (:require [tablecloth.api :refer [dataset]]
            [tablecloth.time.index :refer [index-by]]
            [tablecloth.time.operations :as ops]
            [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tick.alpha.api :as t]
            [clojure.test :refer [deftest is]]))

(deftest slice-by-date
  (let [date-ds (dataset {:A (plus-temporal-amount (t/date "1970-01-01") (range 10) :days)
                     :B (range 10)})]
    (is (= (dataset {:A [(t/date "1970-01-09") (t/date "1970-01-10")]
                     :B [9 10]})
           (-> date-ds (index-by :A) (ops/slice-by-date "1970-01-09" "1970-01-10"))))))

