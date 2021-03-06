(ns tablecloth.time.api.converters-test
  (:require [clojure.test :refer [testing deftest is function?]]
            [tablecloth.time.api :refer [down-to-nearest ->seconds convert-to
                                         ->minutes ->hours ->days ->weeks-end
                                         ->months-end ->quarters-end ->years-end
                                         string->time]]))

(deftest test-down-to-nearest
  (testing "returns partial fn if datetime not provided"
    (is (function? (down-to-nearest 5 :seconds))))

  (testing "returns datetime-type matching datetime given"
    (is (= java.time.Instant
           (type (down-to-nearest 5 :seconds #time/instant "1970-01-01T00:00:07Z"))))
    (is (= java.time.LocalDate
           (type (down-to-nearest 5 :days #time/date "1970-01-01")))))

  (testing "rounds down correctly"
    (is (= #time/date "1970-01-01"
           (down-to-nearest 5 :days #time/date "1970-01-01")))
    (is (= #time/date "1970-01-01"
           (down-to-nearest 5 :days #time/date "1970-01-05")))))

(deftest test->seconds
  (is (= #time/instant "1970-01-01T00:00:01Z"
         (->seconds #time/instant "1970-01-01T00:00:01.100Z"))))

(deftest test->minutes
  (is (= #time/instant "1970-01-01T00:01:00Z"
         (->minutes #time/instant "1970-01-01T00:01:00.000Z"))))

(deftest test->hours
  (is (= #time/instant "1970-01-01T01:00:00Z"
         (->hours #time/instant "1970-01-01T01:00:00.100Z"))))

(deftest test->days
  (is (= #time/date "1970-01-01"
         (->days #time/instant "1970-01-01T01:00:00.100Z"))))

(deftest test->weeks-end
  (is (= #time/date "1970-01-04"
         (->weeks-end #time/instant "1970-01-01T00:00:00.000Z"))))

(deftest test->months-end
  (is (= #time/date "1970-01-31"
         (->months-end #time/date "1970-01-01"))))

(deftest test->quarters-end
  (is (= #time/date "1970-03-31"
         (->quarters-end #time/date "1970-01-01"))))

(deftest test->years-end
  (is (= #time/date "1970-12-31"
         (->years-end #time/date "1970-01-01"))))

(deftest test-convert-to
  (is (= #time/date "1970-01-01"
         (convert-to #time/instant "1970-01-01T00:00:00Z" :local-date)))
  (is (= #time/instant "1970-01-01T00:00:00Z"
         (convert-to #time/date "1970-01-01" :instant))))

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
         (string->time "1970-01-01")))
  (is (= #time/year-month "1970-01"
         (string->time "1970-01")))
  (is (= #time/year "1970"
         (string->time "1970"))))

