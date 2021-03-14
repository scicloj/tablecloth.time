(ns tablecloth.api.conversion-test
  (:require [clojure.test :refer [testing deftest is function?]]
            [tablecloth.time.api.conversion :refer [round-down-to-nearest]]))

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
