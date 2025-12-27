(ns tablecloth.time.column.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.api.parse :refer [parse]]
            [tablecloth.time.column.api :refer [convert-time floor-to-month floor-to-quarter floor-to-year]])
  (:import [java.time Duration Instant LocalDate LocalDateTime ZonedDateTime]))

(deftest convert-time-temporal->epoch-default-utc
  (testing "LocalDate -> :epoch-milliseconds defaults to UTC"
    (let [col (tcc/column [(parse "1970-01-01")
                           (parse "1970-01-02")])
          res (-> (convert-time col :epoch-milliseconds)
                  vec
                  (->> (map long)))]
      (is (= [0 86400000] res)))))

(deftest convert-time-temporal->epoch-explicit-zone
  (testing "LocalDate -> :epoch-milliseconds with explicit non-UTC zone"
    ;; In Europe/Berlin, 1970-01-01T00:00 local is 1969-12-31T23:00Z
    ;; so epoch millis is -3600000.
    (let [col (tcc/column [(parse "1970-01-01")])
          res (-> (convert-time col :epoch-milliseconds {:zone "Europe/Berlin"})
                  vec
                  (->> (map long)))]
      (is (= [-3600000] res)))))

(deftest convert-time-epoch->temporal-roundtrip
  (testing "LocalDate -> epoch-millis -> LocalDate round-trip"
    (let [dates [(parse "1970-01-01")
                 (parse "1970-01-02")]
          col   (tcc/column dates)
          epoch (convert-time col :epoch-milliseconds)
          back  (-> (convert-time epoch :local-date)
                    vec)]
      (is (= dates back)))))

(deftest convert-time-temporal->temporal
  (testing "LocalDate -> Instant via default UTC zone"
    (let [col  (tcc/column [(parse "1970-01-01")])
          inst (first (convert-time col :instant))]
      (is (= (parse "1970-01-01T00:00:00Z") inst)))))

(deftest convert-time-epoch->epoch-scaling
  (testing "epoch-days -> epoch-hours uses numeric scaling"
    (let [dates  [(parse "1970-01-01")
                  (parse "1970-01-03")]
          col    (tcc/column dates)
          days-col (convert-time col :epoch-days)
          hours-col (convert-time days-col :epoch-hours)
          days   (vec days-col)
          hours  (vec hours-col)]
      ;; 1 day == 24 hours in these epoch units
      (is (= (map long (map #(* 24 %) days))
             (map long hours))))))

(deftest convert-time-class-target-and-synonyms
  (testing "class targets and keyword synonyms are normalized correctly"
    (let [inst-col (tcc/column [(Instant/parse "1970-01-01T00:00:00Z")])]
      ;; Class target -> LocalDateTime
      (let [ldt-col (convert-time inst-col LocalDateTime)
            v       (first ldt-col)]
        (is (instance? LocalDateTime v)))
      ;; :zdt synonym -> ZonedDateTime
      (let [zdt-col (convert-time inst-col :zdt)
            v       (first zdt-col)]
        (is (instance? ZonedDateTime v)))
      ;; :ldt synonym -> LocalDateTime
      (let [ldt-col (convert-time inst-col :ldt)
            v       (first ldt-col)]
        (is (instance? LocalDateTime v))))))

(deftest convert-time-unsupported-duration-category
  (testing "duration category is rejected by convert-time"
    (let [dur-col (tcc/column [(Duration/ofDays 1)])]
      (is (try
            (convert-time dur-col :epoch-milliseconds)
            false
            (catch clojure.lang.ExceptionInfo _
              true))))))

(deftest floor-to-month-test
  (testing "floor to 1-month intervals"
    (let [dates [(parse "1970-01-15")
                 (parse "1970-03-20")
                 (parse "1970-06-10")]
          col (tcc/column dates)
          result (floor-to-month col 1)]
      (is (= [(parse "1970-01-01")
              (parse "1970-03-01")
              (parse "1970-06-01")]
             (vec result)))))

  (testing "floor to 6-month intervals"
    (let [dates [(parse "1970-01-15")  ; month 0 -> bucket 0
                 (parse "1970-06-20")  ; month 5 -> bucket 0
                 (parse "1970-07-10")  ; month 6 -> bucket 1
                 (parse "1970-12-31")] ; month 11 -> bucket 1
          col (tcc/column dates)
          result (floor-to-month col 6)]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-07-01")
              (parse "1970-07-01")]
             (vec result)))))

  (testing "floor to 3-month intervals"
    (let [dates [(parse "1970-01-15")  ; month 0 -> bucket 0
                 (parse "1970-03-20")  ; month 2 -> bucket 0
                 (parse "1970-04-10")  ; month 3 -> bucket 1
                 (parse "1970-07-15")] ; month 6 -> bucket 2
          col (tcc/column dates)
          result (floor-to-month col 3)]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-04-01")
              (parse "1970-07-01")]
             (vec result)))))

  (testing "preserves column type - Instant"
    (let [instants [(Instant/parse "1970-03-15T12:30:45Z")
                    (Instant/parse "1970-06-20T08:15:30Z")]
          col (tcc/column instants)
          result (floor-to-month col 1 {:zone "UTC"})]
      (is (every? #(instance? Instant %) result))
      (is (= [(Instant/parse "1970-03-01T00:00:00Z")
              (Instant/parse "1970-06-01T00:00:00Z")]
             (vec result))))))

(deftest floor-to-quarter-test
  (testing "floor to 1-quarter intervals"
    (let [dates [(parse "1970-01-15")  ; Q1
                 (parse "1970-04-20")  ; Q2
                 (parse "1970-07-10")  ; Q3
                 (parse "1970-10-25")] ; Q4
          col (tcc/column dates)
          result (floor-to-quarter col 1)]
      (is (= [(parse "1970-01-01")
              (parse "1970-04-01")
              (parse "1970-07-01")
              (parse "1970-10-01")]
             (vec result)))))

  (testing "floor to 2-quarter intervals (half-years)"
    (let [dates [(parse "1970-01-15")  ; Q1 (quarter 0) -> bucket 0
                 (parse "1970-06-20")  ; Q2 (quarter 1) -> bucket 0
                 (parse "1970-07-10")  ; Q3 (quarter 2) -> bucket 1
                 (parse "1970-12-31")] ; Q4 (quarter 3) -> bucket 1
          col (tcc/column dates)
          result (floor-to-quarter col 2)]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-07-01")
              (parse "1970-07-01")]
             (vec result)))))

  (testing "floor to 4-quarter intervals (years)"
    (let [dates [(parse "1970-01-15")  ; Q0 -> bucket 0 (Q0-3)
                 (parse "1970-12-31")  ; Q3 -> bucket 0 (Q0-3)
                 (parse "1971-03-15")  ; Q4 -> bucket 1 (Q4-7)
                 (parse "1971-12-31")] ; Q7 -> bucket 1 (Q4-7)
          col (tcc/column dates)
          result (floor-to-quarter col 4)]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1971-01-01")  ; Q4 floors to Q4 = 1971-01-01
              (parse "1971-01-01")]
             (vec result)))))

  (testing "quarters spanning multiple years"
    (let [dates [(parse "1971-03-15")  ; Q4 (1971-Q1)
                 (parse "1972-06-20")] ; Q9 (1972-Q2)
          col (tcc/column dates)
          result (floor-to-quarter col 3)]
      ;; Q0=1970-Jan-Mar, Q1=Apr-Jun, Q2=Jul-Sep, Q3=Oct-Dec
      ;; Q4=1971-Jan-Mar, Q5=Apr-Jun, Q6=Jul-Sep, Q7=Oct-Dec
      ;; Q8=1972-Jan-Mar, Q9=Apr-Jun
      ;; With interval 3: buckets are Q0-2, Q3-5, Q6-8, Q9-11
      ;; 1971-Q1 = Q4 -> Q4 - (Q4 % 3) = 4 - 1 = Q3 = 1970-10-01
      ;; 1972-Q2 = Q9 -> Q9 - (Q9 % 3) = 9 - 0 = Q9 = 1972-04-01
      (is (= [(parse "1970-10-01")
              (parse "1972-04-01")]
             (vec result))))))

(deftest floor-to-year-test
  (testing "floor to 1-year intervals"
    (let [dates [(parse "1970-06-15")
                 (parse "1971-03-20")
                 (parse "1972-12-25")]
          col (tcc/column dates)
          result (floor-to-year col 1)]
      (is (= [(parse "1970-01-01")
              (parse "1971-01-01")
              (parse "1972-01-01")]
             (vec result)))))

  (testing "floor to 5-year intervals"
    (let [dates [(parse "1970-06-15")  ; year 0 -> bucket 0
                 (parse "1974-03-20")  ; year 4 -> bucket 0
                 (parse "1975-01-01")  ; year 5 -> bucket 1
                 (parse "1979-12-31")  ; year 9 -> bucket 1
                 (parse "1980-06-15")] ; year 10 -> bucket 2
          col (tcc/column dates)
          result (floor-to-year col 5)]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1975-01-01")
              (parse "1975-01-01")
              (parse "1980-01-01")]
             (vec result)))))

  (testing "floor to 10-year intervals (decades)"
    (let [dates [(parse "1975-06-15")
                 (parse "1989-12-31")
                 (parse "1990-01-01")
                 (parse "2005-06-15")]
          col (tcc/column dates)
          result (floor-to-year col 10)]
      (is (= [(parse "1970-01-01")
              (parse "1980-01-01")
              (parse "1990-01-01")
              (parse "2000-01-01")]
             (vec result)))))

  (testing "preserves column type - LocalDate"
    (let [dates [(LocalDate/of 1972 6 15)]
          col (tcc/column dates)
          result (floor-to-year col 1)]
      (is (every? #(instance? LocalDate %) result)))))
