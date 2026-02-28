(ns tablecloth.time.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [tech.v3.datatype :as dtype]
            [tablecloth.api :as tc]
            [tablecloth.time.api :as time-api])
  (:import [java.time LocalDate LocalDateTime]))

(deftest add-time-columns-vector-test
  (testing "vector form uses field names as column names"
    (let [ds (tc/dataset {:date [(LocalDate/of 2024 3 15)
                                 (LocalDate/of 2024 12 25)
                                 (LocalDate/of 2025 1 1)]
                          :value [1 2 3]})
          result (time-api/add-time-columns ds :date [:year :month :day])]
      (is (= [2024 2024 2025] (vec (result :year))))
      (is (= [3 12 1] (vec (result :month))))
      (is (= [15 25 1] (vec (result :day)))))))

(deftest add-time-columns-map-test
  (testing "map form uses specified column names"
    (let [ds (tc/dataset {:date [(LocalDate/of 2024 3 15)
                                 (LocalDate/of 2024 6 1)]
                          :value [1 2]})
          result (time-api/add-time-columns ds :date {:year :Year
                                                      :month :MonthNum
                                                      :quarter :Q})]
      (is (= [2024 2024] (vec (result :Year))))
      (is (= [3 6] (vec (result :MonthNum))))
      (is (= [1 2] (vec (result :Q)))))))

(deftest add-time-columns-all-fields-test
  (testing "all supported fields extract correctly"
    (let [ds (tc/dataset {:ts [(LocalDateTime/of 2024 6 15 14 30 45)]})
          result (time-api/add-time-columns ds :ts
                                            [:year :month :day :hour :minute :second
                                             :day-of-week :day-of-year :week-of-year :quarter])]
      (is (= [2024] (vec (result :year))))
      (is (= [6] (vec (result :month))))
      (is (= [15] (vec (result :day))))
      (is (= [14] (vec (result :hour))))
      (is (= [30] (vec (result :minute))))
      (is (= [45] (vec (result :second))))
      (is (= [6] (vec (result :day-of-week))))  ;; Saturday
      (is (= [167] (vec (result :day-of-year))))
      (is (= [24] (vec (result :week-of-year))))
      (is (= [2] (vec (result :quarter)))))))

(deftest add-time-columns-unknown-field-test
  (testing "unknown field throws informative error"
    (let [ds (tc/dataset {:date [(LocalDate/of 2024 1 1)]})]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Unknown time field: :bogus"
                            (time-api/add-time-columns ds :date [:bogus]))))))

(deftest add-time-columns-string-col-name-test
  (testing "works with string column names"
    (let [ds (tc/dataset {"Date" [(LocalDate/of 2024 7 4)]})
          result (time-api/add-time-columns ds "Date" [:year :month])]
      (is (= [2024] (vec (result :year))))
      (is (= [7] (vec (result :month)))))))

(deftest add-lag-test
  (testing "lag shifts values forward with nil at start"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0]})
          result (time-api/add-lag ds :a 2 :a_lag2)]
      (is (= [nil nil 1.0 2.0 3.0] (vec (result :a_lag2))))
      (is (= :float64 (dtype/elemwise-datatype (result :a_lag2)))))))

(deftest add-lead-test
  (testing "lead shifts values backward with nil at end"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0]})
          result (time-api/add-lead ds :a 2 :a_lead2)]
      (is (= [3.0 4.0 5.0 nil nil] (vec (result :a_lead2))))
      (is (= :float64 (dtype/elemwise-datatype (result :a_lead2)))))))

(deftest add-lags-vector-test
  (testing "add-lags with vector auto-names and drops missing by default"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0 6.0]})
          result (time-api/add-lags ds :a [2 4])]
      ;; Should have dropped first 4 rows (max lag)
      (is (= 2 (tc/row-count result)))
      ;; Auto-named columns
      (is (= [3.0 4.0] (vec (result :a_lag2))))
      (is (= [1.0 2.0] (vec (result :a_lag4)))))))

(deftest add-lags-map-test
  (testing "add-lags with map uses custom names"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0 6.0]})
          result (time-api/add-lags ds :a {2 :short 4 :long})]
      (is (= [3.0 4.0] (vec (result :short))))
      (is (= [1.0 2.0] (vec (result :long)))))))

(deftest add-lags-no-drop-test
  (testing "add-lags with :drop-missing false keeps nils"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0]})
          result (time-api/add-lags ds :a [2] {:drop-missing false})]
      (is (= 5 (tc/row-count result)))
      (is (= [nil nil 1.0 2.0 3.0] (vec (result :a_lag2)))))))

(deftest add-leads-vector-test
  (testing "add-leads with vector auto-names and drops missing by default"
    (let [ds (tc/dataset {:a [1.0 2.0 3.0 4.0 5.0 6.0]})
          result (time-api/add-leads ds :a [2 4])]
      ;; Should have dropped last 4 rows (max lead)
      (is (= 2 (tc/row-count result)))
      ;; Auto-named columns
      (is (= [3.0 4.0] (vec (result :a_lead2))))
      (is (= [5.0 6.0] (vec (result :a_lead4)))))))
