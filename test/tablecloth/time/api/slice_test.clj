(ns tablecloth.time.api.slice-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.api :as tc]
            [tablecloth.time.api.slice :as slice]
            [tablecloth.time.time-literals]))

(deftest test-slice-basic-ascending
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-01"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-10"
                                    #time/date "2024-01-15"
                                    #time/date "2024-01-20"
                                    #time/date "2024-01-25"
                                    #time/date "2024-01-31"]
                        :value [10 20 30 40 50 60 70]})]
    (testing "slices ascending data with string dates"
      (let [result (slice/slice ds :timestamp "2024-01-07" "2024-01-18")]
        (is (= 2 (tc/row-count result)))
        (is (= [30 40] (vec (:value result))))))

    (testing "slices to include exact boundary matches"
      (let [result (slice/slice ds :timestamp "2024-01-10" "2024-01-20")]
        (is (= 3 (tc/row-count result)))
        (is (= [30 40 50] (vec (:value result))))))

    (testing "slices entire range"
      (let [result (slice/slice ds :timestamp "2024-01-01" "2024-01-31")]
        (is (= 7 (tc/row-count result)))
        (is (= [10 20 30 40 50 60 70] (vec (:value result))))))

    (testing "returns empty dataset when no rows in range"
      (let [result (slice/slice ds :timestamp "2024-02-01" "2024-02-28")]
        (is (= 0 (tc/row-count result)))))

    (testing "returns empty dataset when range is before all data"
      (let [result (slice/slice ds :timestamp "2023-01-01" "2023-12-31")]
        (is (= 0 (tc/row-count result)))))))

(deftest test-slice-with-time-literals
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-01"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-10"
                                    #time/date "2024-01-15"
                                    #time/date "2024-01-20"
                                    #time/date "2024-01-25"
                                    #time/date "2024-01-31"]
                        :value [10 20 30 40 50 60 70]})]
    (testing "works with #time/date literals"
      (let [result (slice/slice ds :timestamp
                                #time/date "2024-01-10"
                                #time/date "2024-01-20")]
        (is (= 3 (tc/row-count result)))
        (is (= [30 40 50] (vec (:value result))))))))

(deftest test-slice-descending
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-31"
                                    #time/date "2024-01-25"
                                    #time/date "2024-01-20"
                                    #time/date "2024-01-15"
                                    #time/date "2024-01-10"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-01"]
                        :value [70 60 50 40 30 20 10]})]
    (testing "slices descending data"
      (let [result (slice/slice ds :timestamp "2024-01-07" "2024-01-18")]
        (is (= 2 (tc/row-count result)))
        ;; Descending data, so values are in reverse order
        (is (= [40 30] (vec (:value result))))))))

(deftest test-slice-multi-month
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-15"
                                    #time/date "2024-02-10"
                                    #time/date "2024-03-05"
                                    #time/date "2024-04-20"
                                    #time/date "2024-05-12"]
                        :value [100 200 300 400 500]})]
    (testing "slices across multiple months"
      (let [result (slice/slice ds :timestamp
                                "2024-02-01"
                                "2024-04-30")]
        (is (= 3 (tc/row-count result)))
        (is (= [200 300 400] (vec (:value result))))))))

(deftest test-slice-single-row
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-15"]
                        :value [42]})]
    (testing "slices single-row dataset when in range"
      (let [result (slice/slice ds :timestamp
                                "2024-01-01"
                                "2024-01-31")]
        (is (= 1 (tc/row-count result)))
        (is (= [42] (vec (:value result))))))

    (testing "returns empty when single row is out of range"
      (let [result (slice/slice ds :timestamp
                                "2024-02-01"
                                "2024-02-28")]
        (is (= 0 (tc/row-count result)))))))

(deftest test-slice-duplicates
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-01"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-10"]
                        :value [10 20 21 22 30]})]
    (testing "includes all duplicate timestamps in range"
      (let [result (slice/slice ds :timestamp
                                "2024-01-05"
                                "2024-01-05")]
        (is (= 3 (tc/row-count result)))
        (is (= [20 21 22] (vec (:value result))))))))

(deftest test-slice-result-type
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-01"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-10"
                                    #time/date "2024-01-15"
                                    #time/date "2024-01-20"
                                    #time/date "2024-01-25"
                                    #time/date "2024-01-31"]
                        :value [10 20 30 40 50 60 70]})]
    (testing "returns dataset by default"
      (let [result (slice/slice ds :timestamp "2024-01-10" "2024-01-20")]
        (is (tc/dataset? result))))

    (testing "returns indices with :as-indices option"
      (let [result (slice/slice ds :timestamp "2024-01-10" "2024-01-20"
                                {:result-type :as-indices})]
        (is (not (tc/dataset? result)))
        (is (= [2 3 4] (vec result)))))))

(deftest test-slice-errors
  (let [ds (tc/dataset {:timestamp [#time/date "2024-01-01"
                                    #time/date "2024-01-05"
                                    #time/date "2024-01-10"
                                    #time/date "2024-01-15"
                                    #time/date "2024-01-20"
                                    #time/date "2024-01-25"
                                    #time/date "2024-01-31"]
                        :value [10 20 30 40 50 60 70]})]
    (testing "throws error when from > to"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"from.*must be less than or equal to.*to"
           (slice/slice ds :timestamp "2024-01-31" "2024-01-01"))))

    (testing "throws error when time column doesn't exist"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Time column is nil"
           (slice/slice ds :nonexistent "2024-01-01" "2024-01-31"))))))
