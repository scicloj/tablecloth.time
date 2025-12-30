(ns tablecloth.time.api.slice-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.api :as tc]
            [tablecloth.time.api.slice :as s]
            [tablecloth.time.time-literals]))

def ds-asc
  (tc/dataset {:timestamp [#time/date "2024-01-01"
                          #time/date "2024-01-05"
                          #time/date "2024-01-10"
                          #time/date "2024-01-15"
                          #time/date "2024-01-20"
                          #time/date "2024-01-25"
                          #time/date "2024-01-31"]
               :value [10 20 30 40 50 60 70]}))

(deftest test-slice
  (testing "works for sorted column with asc sort order"
    (is (= (s/slice ds-asc :timestamp "2024-01-07" "2024-01-18")
           (tc/dataset {:timestamp [#time/date "2024-01-10"
                                    #time/date "2024-01-15"]
                        :value [30 40]})))))

