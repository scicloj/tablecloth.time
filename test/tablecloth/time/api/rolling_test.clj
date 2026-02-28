(ns tablecloth.time.api.rolling-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.api :as tc]
            [tablecloth.time.api.rolling :as r]
            [tablecloth.time.time-literals]))

(def stocks-ds
  (tc/dataset
   {:symbol ["AAPL" "AAPL" "AAPL" "AAPL" "AAPL"
             "MSFT" "MSFT" "MSFT" "MSFT" "MSFT"]
    :date   [#time/date "2024-01-01"
             #time/date "2024-01-02"
             #time/date "2024-01-03"
             #time/date "2024-01-05"   ;; gap: 4th missing
             #time/date "2024-01-08"   ;; gap: weekend
             #time/date "2024-01-01"
             #time/date "2024-01-02"
             #time/date "2024-01-04"   ;; gap: 3rd missing
             #time/date "2024-01-05"
             #time/date "2024-01-08"]
    :close  [180.0 182.5 181.0 183.0 185.0
             330.0 332.0 331.5 333.0 334.5]}))

(deftest test-rolling
  (testing "rolling is not yet implemented"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"not implemented"
                          (r/rolling stocks-ds
                                     :date
                                     {:window [2 :days]}
                                     {:ma20 (fn [ds] (tc/mean ds :close))})))))


