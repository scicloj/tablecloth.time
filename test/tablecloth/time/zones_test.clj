(ns tablecloth.time.zones-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.time.zones :as zones]))

(deftest available-time-zones-test
  (testing "returns a sorted vector of time zone IDs"
    (let [tzs (zones/available-time-zones)]
      (is (vector? tzs))
      (is (> (count tzs) 500))
      (is (some #{"UTC" "America/New_York" "Europe/London"} tzs))
      (is (= tzs (sort tzs))))))

(deftest find-time-zones-test
  (testing "finds time zones by case-insensitive search"
    (is (= ["America/New_York"] (zones/find-time-zones "new york")))
    (is (= ["America/New_York"] (zones/find-time-zones "NEW YORK")))
    (is (= ["Europe/Berlin"] (zones/find-time-zones "berlin"))))

  (testing "returns multiple matches"
    (let [results (zones/find-time-zones "america")]
      (is (> (count results) 10))
      (is (some #{"America/New_York" "America/Chicago"} results))))

  (testing "returns empty vector when no matches"
    (is (= [] (zones/find-time-zones "nonexistent-zone-xyz")))))

(deftest time-zone-info-test
  (testing "returns info map with expected keys"
    (let [info (zones/time-zone-info "America/New_York")]
      (is (map? info))
      (is (= "America/New_York" (:id info)))
      (is (string? (:offset info)))
      (is (boolean? (:dst? info)))
      (is (string? (:display-name info)))))

  (testing "UTC has zero offset and no DST"
    (let [info (zones/time-zone-info "UTC")]
      (is (= "UTC" (:id info)))
      (is (= "Z" (:offset info)))
      (is (false? (:dst? info))))))

(deftest common-time-zones-test
  (testing "contains expected common zones"
    (is (some #{"UTC" "America/New_York" "Europe/London"} zones/common-time-zones))
    (is (> (count zones/common-time-zones) 5))))
