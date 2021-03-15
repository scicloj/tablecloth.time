(ns tablecloth.api.conversion-test
  (:require [clojure.test :refer [testing deftest is function? are]]
            [tablecloth.time.api.conversion :refer [round-down-to-nearest ->seconds
                                                    ->minutes ->hours ->days ->weeks
                                                    ->months ->years]]))

(deftest test-round-down-to-nearest
  (testing "returns partial fn if datetime not provided"
    (is (function? (round-down-to-nearest 5 :seconds))))

  (testing "returns datetime-type matching datetime given"
    (is (= java.time.Instant
           (type (round-down-to-nearest 5 :seconds #time/instant "1970-01-01T00:00:07Z"))))
    (is (= java.time.LocalDate
           (type (round-down-to-nearest 5 :days #time/date "1970-01-01")))))

  (testing "rounds down correctly"
    (is (= #time/date "1970-01-01"
           (round-down-to-nearest 5 :days #time/date "1970-01-01")))
    (is (= #time/date "1970-01-01"
           (round-down-to-nearest 5 :days #time/date "1970-01-05")))))

(deftest test->seconds
  (is (= #time/instant "1970-01-01T00:00:01Z"
         (->seconds #time/instant "1970-01-01T00:00:01.100Z"))))

(deftest test->minutes
  (is (= #time/instant "1970-01-01T00:01:00Z"
         (->minutes #time/instant "1970-01-01T00:01:00.000Z"))))

(deftest test->minutes
  (is (= #time/instant "1970-01-01T01:00:00Z"
         (->hours #time/instant "1970-01-01T01:00:00.100Z"))))

(deftest test->days
  (is (= #time/date "1970-01-01"
         (->days #time/instant "1970-01-01T01:00:00.100Z"))))

(deftest test->weeks
  (is (= #time/date "1970-01-04"
         (->weeks #time/instant "1970-01-01T00:00:00.000Z"))))

(deftest test->months
  (is (= #time/date "1970-01-31"
         (->months #time/date "1970-01-01"))))

(deftest test->years
  (is (= #time/date "1970-12-31"
         (->years #time/date "1970-01-01"))))

