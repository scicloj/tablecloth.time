(ns tablecloth.time.api.parse-test
  (:require [tablecloth.time.api :refer [string->time]]
            [clojure.test :refer [deftest is]]))

(deftest test-string->time
  (is (= #time/time "01:00"
         (string->time "1")))
  (is (= #time/time "10:00"
         (string->time "10")))
  (is (= #time/time "09:30"
         (string->time "9:30")))
  (is (= #time/time "09:30"
         (string->time "09:30")))
  (is (= #time/instant "1970-01-01T00:00:00Z"
         (string->time "1970-01-01T00:00:00Z")))
  (is (= #time/instant "1970-01-01T00:00:00.000Z"
         (string->time "1970-01-01T00:00:00.000Z")))
  (is (= #time/offset-date-time "1970-01-01T00:00:00+01:00"
         (string->time "1970-01-01T00:00:00+01:00")))
  (is (= #time/date-time "1970-01-01T00:00"
         (string->time "1970-01-01T00:00")))
  (is (= #time/date "1970-01-01"
         (string->time "1970-01-01"))))



