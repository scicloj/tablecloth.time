(ns tablecloth.time.api.time-components-test
  (:require [clojure.test :refer [testing deftest is function?]]
            [tablecloth.time.api.time-components :refer [year dayofyear
                                                         month dayofmonth
                                                         dayofweek hour
                                                         minute secnd]]))

(deftest test-year
  (is (= 1970 (year #time/date "1970-01-01"))))

(deftest test-dayofyear
  (is (= 1 (dayofyear #time/date "1970-01-01"))))

(deftest test-month
  (is (= "JANUARY" (month #time/date "1970-01-01")))
  (is (= 1 (month #time/date "1970-01-01" {:as-number? true})))
  (is (= (.getMonth #time/date "1970-01-01")
         (month #time/date "1970-01-01" {:as-class? true}))))

(deftest test-dayofmonth
  (is (= 1 (dayofmonth #time/date "1970-01-01"))))

(deftest test-dayofweek
  (is (= "THURSDAY" (dayofweek #time/date "1970-01-01")))
  (is (= 4 (dayofweek #time/date "1970-01-01" {:as-number? true})))
  (is (= (.getDayOfWeek #time/date "1970-01-01")
         (dayofweek #time/date "1970-01-01" {:as-class? true}))))

(deftest test-hour
  (is (= 13 (hour #time/date-time "1970-01-01T13:13"))))

(deftest test-minute
  (is (= 13 (minute #time/date-time "1970-01-01T13:13"))))

(deftest test-secnd
  (is (= 13 (secnd #time/instant "1970-01-01T13:13:13Z"))))

