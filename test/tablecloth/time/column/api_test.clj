(ns tablecloth.time.column.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.parse :refer [parse]]
            [tablecloth.time.column.api :refer [convert-time down-to-nearest floor-to-month floor-to-quarter floor-to-year
                                                year month day hour minute get-second
                                                day-of-week day-of-year week-of-year quarter]])
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

(deftest down-to-nearest-milliseconds-test
  (testing "floor to 1000 milliseconds (1 second)"
    (let [instants [(Instant/parse "1970-01-01T00:00:00.123Z")
                    (Instant/parse "1970-01-01T00:00:00.999Z")
                    (Instant/parse "1970-01-01T00:00:01.000Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1000 :milliseconds {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00.000Z")
              (Instant/parse "1970-01-01T00:00:00.000Z")
              (Instant/parse "1970-01-01T00:00:01.000Z")]
             (vec result)))))

  (testing "floor to 500 milliseconds"
    (let [instants [(Instant/parse "1970-01-01T00:00:00.123Z")
                    (Instant/parse "1970-01-01T00:00:00.499Z")
                    (Instant/parse "1970-01-01T00:00:00.500Z")
                    (Instant/parse "1970-01-01T00:00:00.999Z")]
          col (tcc/column instants)
          result (down-to-nearest col 500 :milliseconds {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00.000Z")
              (Instant/parse "1970-01-01T00:00:00.000Z")
              (Instant/parse "1970-01-01T00:00:00.500Z")
              (Instant/parse "1970-01-01T00:00:00.500Z")]
             (vec result))))))

(deftest down-to-nearest-seconds-test
  (testing "floor to 30 seconds"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T00:00:15Z")
                    (Instant/parse "1970-01-01T00:00:30Z")
                    (Instant/parse "1970-01-01T00:00:45Z")
                    (Instant/parse "1970-01-01T00:01:00Z")]
          col (tcc/column instants)
          result (down-to-nearest col 30 :seconds {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:30Z")
              (Instant/parse "1970-01-01T00:00:30Z")
              (Instant/parse "1970-01-01T00:01:00Z")]
             (vec result)))))

  (testing "floor to 1 second"
    (let [instants [(Instant/parse "1970-01-01T00:00:00.000Z")
                    (Instant/parse "1970-01-01T00:00:00.500Z")
                    (Instant/parse "1970-01-01T00:00:01.999Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1 :second {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:01Z")]
             (vec result))))))

(deftest down-to-nearest-minutes-test
  (testing "floor to 15 minutes"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T00:07:30Z")
                    (Instant/parse "1970-01-01T00:15:00Z")
                    (Instant/parse "1970-01-01T00:22:45Z")
                    (Instant/parse "1970-01-01T00:45:00Z")]
          col (tcc/column instants)
          result (down-to-nearest col 15 :minutes {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:15:00Z")
              (Instant/parse "1970-01-01T00:15:00Z")
              (Instant/parse "1970-01-01T00:45:00Z")]
             (vec result)))))

  (testing "floor to 1 minute"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T00:00:30Z")
                    (Instant/parse "1970-01-01T00:01:59Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1 :minute {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:01:00Z")]
             (vec result))))))

(deftest down-to-nearest-hours-test
  (testing "floor to 6 hours"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T05:59:59Z")
                    (Instant/parse "1970-01-01T06:00:00Z")
                    (Instant/parse "1970-01-01T11:30:00Z")
                    (Instant/parse "1970-01-01T18:00:00Z")]
          col (tcc/column instants)
          result (down-to-nearest col 6 :hours {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T06:00:00Z")
              (Instant/parse "1970-01-01T06:00:00Z")
              (Instant/parse "1970-01-01T18:00:00Z")]
             (vec result)))))

  (testing "floor to 1 hour"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T00:59:59Z")
                    (Instant/parse "1970-01-01T01:00:00Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1 :hour {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T01:00:00Z")]
             (vec result))))))

(deftest down-to-nearest-days-test
  (testing "floor to 7 days (1 week)"
    (let [dates [(parse "1970-01-01")  ; Thursday (epoch day 0)
                 (parse "1970-01-07")  ; Wednesday (epoch day 6)
                 (parse "1970-01-08")  ; Thursday (epoch day 7)
                 (parse "1970-01-14")] ; Wednesday (epoch day 13)
          col (tcc/column dates)
          result (down-to-nearest col 7 :days {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-01-08")
              (parse "1970-01-08")]
             (vec result)))))

  (testing "floor to 1 day"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T23:59:59Z")
                    (Instant/parse "1970-01-02T00:00:00Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1 :day {:zone "UTC"})]
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-01T00:00:00Z")
              (Instant/parse "1970-01-02T00:00:00Z")]
             (vec result))))))

(deftest down-to-nearest-weeks-test
  (testing "floor to 2 weeks"
    (let [dates [(parse "1970-01-01")  ; day 0
                 (parse "1970-01-14")  ; day 13
                 (parse "1970-01-15")  ; day 14
                 (parse "1970-01-28")  ; day 27
                 (parse "1970-01-29")] ; day 28
          col (tcc/column dates)
          result (down-to-nearest col 2 :weeks {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-01-15")
              (parse "1970-01-15")
              (parse "1970-01-29")]
             (vec result)))))

  (testing "floor to 1 week"
    (let [dates [(parse "1970-01-01")  ; day 0
                 (parse "1970-01-07")  ; day 6
                 (parse "1970-01-08")] ; day 7
          col (tcc/column dates)
          result (down-to-nearest col 1 :week {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-01-08")]
             (vec result))))))

(deftest down-to-nearest-months-test
  (testing "floor to 1 month"
    (let [dates [(parse "1970-01-15")
                 (parse "1970-03-20")
                 (parse "1970-06-10")]
          col (tcc/column dates)
          result (down-to-nearest col 1 :months {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-03-01")
              (parse "1970-06-01")]
             (vec result)))))

  (testing "floor to 6 months"
    (let [dates [(parse "1970-01-15")  ; month 0 -> bucket 0
                 (parse "1970-06-20")  ; month 5 -> bucket 0
                 (parse "1970-07-10")  ; month 6 -> bucket 1
                 (parse "1970-12-31")] ; month 11 -> bucket 1
          col (tcc/column dates)
          result (down-to-nearest col 6 :month {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-07-01")
              (parse "1970-07-01")]
             (vec result)))))

  (testing "floor to 3 months"
    (let [dates [(parse "1970-01-15")  ; month 0 -> bucket 0
                 (parse "1970-03-20")  ; month 2 -> bucket 0
                 (parse "1970-04-10")  ; month 3 -> bucket 1
                 (parse "1970-07-15")] ; month 6 -> bucket 2
          col (tcc/column dates)
          result (down-to-nearest col 3 :months {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-04-01")
              (parse "1970-07-01")]
             (vec result))))))

(deftest down-to-nearest-quarters-test
  (testing "floor to 1 quarter"
    (let [dates [(parse "1970-01-15")  ; Q1
                 (parse "1970-04-20")  ; Q2
                 (parse "1970-07-10")  ; Q3
                 (parse "1970-10-25")] ; Q4
          col (tcc/column dates)
          result (down-to-nearest col 1 :quarters {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-04-01")
              (parse "1970-07-01")
              (parse "1970-10-01")]
             (vec result)))))

  (testing "floor to 2 quarters (half-years)"
    (let [dates [(parse "1970-01-15")  ; Q1 (quarter 0) -> bucket 0
                 (parse "1970-06-20")  ; Q2 (quarter 1) -> bucket 0
                 (parse "1970-07-10")  ; Q3 (quarter 2) -> bucket 1
                 (parse "1970-12-31")] ; Q4 (quarter 3) -> bucket 1
          col (tcc/column dates)
          result (down-to-nearest col 2 :quarter {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1970-07-01")
              (parse "1970-07-01")]
             (vec result)))))

  (testing "quarters spanning multiple years"
    (let [dates [(parse "1971-03-15")  ; Q4 (1971-Q1)
                 (parse "1972-06-20")] ; Q9 (1972-Q2)
          col (tcc/column dates)
          result (down-to-nearest col 3 :quarters {:zone "UTC"})]
      (is (= [(parse "1970-10-01")
              (parse "1972-04-01")]
             (vec result))))))

(deftest down-to-nearest-years-test
  (testing "floor to 1 year"
    (let [dates [(parse "1970-06-15")
                 (parse "1971-03-20")
                 (parse "1972-12-25")]
          col (tcc/column dates)
          result (down-to-nearest col 1 :years {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1971-01-01")
              (parse "1972-01-01")]
             (vec result)))))

  (testing "floor to 5 years"
    (let [dates [(parse "1970-06-15")  ; year 0 -> bucket 0
                 (parse "1974-03-20")  ; year 4 -> bucket 0
                 (parse "1975-01-01")  ; year 5 -> bucket 1
                 (parse "1979-12-31")  ; year 9 -> bucket 1
                 (parse "1980-06-15")] ; year 10 -> bucket 2
          col (tcc/column dates)
          result (down-to-nearest col 5 :year {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-01-01")
              (parse "1975-01-01")
              (parse "1975-01-01")
              (parse "1980-01-01")]
             (vec result)))))

  (testing "floor to 10 years (decades)"
    (let [dates [(parse "1975-06-15")
                 (parse "1989-12-31")
                 (parse "1990-01-01")
                 (parse "2005-06-15")]
          col (tcc/column dates)
          result (down-to-nearest col 10 :years {:zone "UTC"})]
      (is (= [(parse "1970-01-01")
              (parse "1980-01-01")
              (parse "1990-01-01")
              (parse "2000-01-01")]
             (vec result))))))

(deftest down-to-nearest-type-preservation-test
  (testing "preserves Instant type"
    (let [instants [(Instant/parse "1970-03-15T12:30:45Z")
                    (Instant/parse "1970-06-20T08:15:30Z")]
          col (tcc/column instants)
          result (down-to-nearest col 1 :months {:zone "UTC"})]
      (is (every? #(instance? Instant %) result))
      (is (= [(Instant/parse "1970-03-01T00:00:00Z")
              (Instant/parse "1970-06-01T00:00:00Z")]
             (vec result)))))

  (testing "preserves LocalDate type"
    (let [dates [(LocalDate/of 1972 6 15)
                 (LocalDate/of 1975 3 20)]
          col (tcc/column dates)
          result (down-to-nearest col 1 :years {:zone "UTC"})]
      (is (every? #(instance? LocalDate %) result))
      (is (= [(LocalDate/of 1972 1 1)
              (LocalDate/of 1975 1 1)]
             (vec result)))))

  (testing "preserves LocalDateTime type"
    (let [datetimes [(LocalDateTime/of 1970 1 1 12 30 45)
                     (LocalDateTime/of 1970 1 1 18 45 30)]
          col (tcc/column datetimes)
          result (down-to-nearest col 6 :hours {:zone "UTC"})]
      (is (every? #(instance? LocalDateTime %) result))
      (is (= [(LocalDateTime/of 1970 1 1 12 0 0)
              (LocalDateTime/of 1970 1 1 18 0 0)]
             (vec result)))))

  (testing "preserves ZonedDateTime type"
    (let [zoned-times [(ZonedDateTime/parse "1970-01-01T12:30:45Z")
                       (ZonedDateTime/parse "1970-01-01T18:45:30Z")]
          col (tcc/column zoned-times)
          result (down-to-nearest col 30 :minutes {:zone "UTC"})]
      (is (every? #(instance? ZonedDateTime %) result))
      ;; Check the instant values are correct, zone representation may vary
      (is (= [(Instant/parse "1970-01-01T12:30:00Z")
              (Instant/parse "1970-01-01T18:30:00Z")]
             (mapv #(.toInstant %) result))))))

(deftest down-to-nearest-with-zones-test
  (testing "LocalDate with explicit non-UTC zone for calendar units"
    ;; Calendar units use the zone differently - test with months
    (let [dates [(parse "1970-01-15")
                 (parse "1970-02-20")]
          col (tcc/column dates)
          result (down-to-nearest col 1 :months {:zone "Europe/Berlin"})]
      (is (= [(parse "1970-01-01")
              (parse "1970-02-01")]
             (vec result)))))

  (testing "Instant with different zones produces same result"
    ;; Instant is absolute time, so zone shouldn't affect metric unit rounding
    (let [instants [(Instant/parse "1970-01-01T00:30:00Z")]
          col (tcc/column instants)
          result-utc (down-to-nearest col 1 :hours {:zone "UTC"})
          result-berlin (down-to-nearest col 1 :hours {:zone "Europe/Berlin"})]
      (is (= (vec result-utc) (vec result-berlin)))
      (is (= [(Instant/parse "1970-01-01T00:00:00Z")]
             (vec result-utc))))))

;; -----------------------------------------------------------------------------
;; Field extractors tests
;; -----------------------------------------------------------------------------

(deftest year-test
  (testing "extract year from LocalDate"
    (let [dates [(parse "1970-01-15")
                 (parse "1999-12-31")
                 (parse "2000-01-01")
                 (parse "2024-06-15")]
          col (tcc/column dates)
          result (year col)]
      (is (= [1970 1999 2000 2024] (vec result)))))

  (testing "extract year from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "2024-12-31T23:59:59Z")]
          col (tcc/column instants)
          result (year col)]
      (is (= [1970 2024] (vec result)))))

  (testing "extract year from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 2024 6 15 12 30 45)]
          col (tcc/column datetimes)
          result (year col)]
      (is (= [1970 2024] (vec result))))))

(deftest month-test
  (testing "extract month from LocalDate"
    (let [dates [(parse "1970-01-15")
                 (parse "1970-02-20")
                 (parse "1970-06-10")
                 (parse "1970-12-31")]
          col (tcc/column dates)
          result (month col)]
      (is (= [1 2 6 12] (vec result)))))

  (testing "extract month from Instant"
    (let [instants [(Instant/parse "1970-03-01T00:00:00Z")
                    (Instant/parse "1970-08-15T12:30:00Z")]
          col (tcc/column instants)
          result (month col)]
      (is (= [3 8] (vec result)))))

  (testing "extract month from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 7 15 12 30 45)]
          col (tcc/column datetimes)
          result (month col)]
      (is (= [1 7] (vec result))))))

(deftest day-test
  (testing "extract day of month from LocalDate"
    (let [dates [(parse "1970-01-01")
                 (parse "1970-01-15")
                 (parse "1970-01-31")
                 (parse "1970-02-28")]
          col (tcc/column dates)
          result (day col)]
      (is (= [1 15 31 28] (vec result)))))

  (testing "extract day from Instant"
    (let [instants [(Instant/parse "1970-01-05T00:00:00Z")
                    (Instant/parse "1970-01-25T23:59:59Z")]
          col (tcc/column instants)
          result (day col)]
      (is (= [5 25] (vec result)))))

  (testing "extract day from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 1 20 12 30 45)]
          col (tcc/column datetimes)
          result (day col)]
      (is (= [1 20] (vec result))))))

(deftest hour-test
  (testing "extract hour from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 1 1 6 30 0)
                     (LocalDateTime/of 1970 1 1 12 0 0)
                     (LocalDateTime/of 1970 1 1 23 59 59)]
          col (tcc/column datetimes)
          result (hour col)]
      (is (= [0 6 12 23] (vec result)))))

  (testing "extract hour from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T15:30:00Z")]
          col (tcc/column instants)
          result (hour col)]
      (is (= [0 15] (vec result)))))

  (testing "extract hour from ZonedDateTime"
    (let [zoned-times [(ZonedDateTime/parse "1970-01-01T08:00:00Z")
                       (ZonedDateTime/parse "1970-01-01T20:30:00Z")]
          col (tcc/column zoned-times)
          result (hour col)]
      (is (= [8 20] (vec result))))))

(deftest minute-test
  (testing "extract minute from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 1 1 12 15 0)
                     (LocalDateTime/of 1970 1 1 12 30 0)
                     (LocalDateTime/of 1970 1 1 12 59 59)]
          col (tcc/column datetimes)
          result (minute col)]
      (is (= [0 15 30 59] (vec result)))))

  (testing "extract minute from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T12:45:00Z")]
          col (tcc/column instants)
          result (minute col)]
      (is (= [0 45] (vec result))))))

(deftest get-second-test
  (testing "extract second from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 1 1 12 30 15)
                     (LocalDateTime/of 1970 1 1 12 30 45)
                     (LocalDateTime/of 1970 1 1 12 30 59)]
          col (tcc/column datetimes)
          result (get-second col)]
      (is (= [0 15 45 59] (vec result)))))

  (testing "extract second from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-01-01T12:30:42Z")]
          col (tcc/column instants)
          result (get-second col)]
      (is (= [0 42] (vec result))))))

(deftest day-of-week-test
  (testing "extract day of week from LocalDate (ISO: Monday=1, Sunday=7)"
    ;; 1970-01-01 is Thursday
    ;; 1970-01-02 is Friday
    ;; 1970-01-03 is Saturday
    ;; 1970-01-04 is Sunday
    ;; 1970-01-05 is Monday
    (let [dates [(parse "1970-01-01")  ; Thursday
                 (parse "1970-01-02")  ; Friday
                 (parse "1970-01-03")  ; Saturday
                 (parse "1970-01-04")  ; Sunday
                 (parse "1970-01-05")] ; Monday
          col (tcc/column dates)
          result (day-of-week col)]
      (is (= [4 5 6 7 1] (vec result)))))

  (testing "extract day of week from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")  ; Thursday
                    (Instant/parse "1970-01-05T12:30:00Z")] ; Monday
          col (tcc/column instants)
          result (day-of-week col)]
      (is (= [4 1] (vec result)))))

  (testing "extract day of week from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)   ; Thursday
                     (LocalDateTime/of 1970 1 4 12 30 0)] ; Sunday
          col (tcc/column datetimes)
          result (day-of-week col)]
      (is (= [4 7] (vec result))))))

(deftest day-of-year-test
  (testing "extract day of year from LocalDate"
    ;; January 1 = day 1
    ;; January 31 = day 31
    ;; February 1 = day 32
    ;; December 31 (non-leap) = day 365
    (let [dates [(parse "1970-01-01")  ; day 1
                 (parse "1970-01-31")  ; day 31
                 (parse "1970-02-01")  ; day 32
                 (parse "1970-12-31")] ; day 365
          col (tcc/column dates)
          result (day-of-year col)]
      (is (= [1 31 32 365] (vec result)))))

  (testing "extract day of year from leap year"
    ;; 2000 is a leap year, so December 31 = day 366
    (let [dates [(parse "2000-02-29")  ; day 60 (leap day)
                 (parse "2000-12-31")] ; day 366
          col (tcc/column dates)
          result (day-of-year col)]
      (is (= [60 366] (vec result)))))

  (testing "extract day of year from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-12-31T23:59:59Z")]
          col (tcc/column instants)
          result (day-of-year col)]
      (is (= [1 365] (vec result))))))

(deftest week-of-year-test
  (testing "extract ISO week of year from LocalDate"
    ;; ISO 8601: Week 1 is the first week with a Thursday
    ;; 1970-01-01 (Thursday) is in week 1
    ;; 1970-01-05 (Monday) is in week 2
    (let [dates [(parse "1970-01-01")  ; week 1
                 (parse "1970-01-05")  ; week 2
                 (parse "1970-06-15")  ; mid-year
                 (parse "1970-12-28")] ; week 53
          col (tcc/column dates)
          result (week-of-year col)]
      (is (= [1 2 25 53] (vec result)))))

  (testing "extract week of year from Instant"
    (let [instants [(Instant/parse "1970-01-01T00:00:00Z")
                    (Instant/parse "1970-06-15T12:30:00Z")]
          col (tcc/column instants)
          result (week-of-year col)]
      (is (= [1 25] (vec result)))))

  (testing "extract week of year from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 1 1 0 0 0)
                     (LocalDateTime/of 1970 1 5 12 30 0)]
          col (tcc/column datetimes)
          result (week-of-year col)]
      (is (= [1 2] (vec result))))))

(deftest quarter-test
  (testing "extract quarter from LocalDate"
    (let [dates [(parse "1970-01-15")  ; Q1
                 (parse "1970-02-20")  ; Q1
                 (parse "1970-03-31")  ; Q1
                 (parse "1970-04-01")  ; Q2
                 (parse "1970-05-15")  ; Q2
                 (parse "1970-06-30")  ; Q2
                 (parse "1970-07-01")  ; Q3
                 (parse "1970-08-20")  ; Q3
                 (parse "1970-09-30")  ; Q3
                 (parse "1970-10-01")  ; Q4
                 (parse "1970-11-15")  ; Q4
                 (parse "1970-12-31")] ; Q4
          col (tcc/column dates)
          result (quarter col)]
      (is (= [1 1 1 2 2 2 3 3 3 4 4 4] (vec result)))))

  (testing "extract quarter from Instant"
    (let [instants [(Instant/parse "1970-01-15T00:00:00Z")  ; Q1
                    (Instant/parse "1970-04-15T00:00:00Z")  ; Q2
                    (Instant/parse "1970-07-15T00:00:00Z")  ; Q3
                    (Instant/parse "1970-10-15T00:00:00Z")] ; Q4
          col (tcc/column instants)
          result (quarter col)]
      (is (= [1 2 3 4] (vec result)))))

  (testing "extract quarter from LocalDateTime"
    (let [datetimes [(LocalDateTime/of 1970 2 1 0 0 0)  ; Q1
                     (LocalDateTime/of 1970 5 1 12 30 0) ; Q2
                     (LocalDateTime/of 1970 8 1 12 30 0) ; Q3
                     (LocalDateTime/of 1970 11 1 12 30 0)] ; Q4
          col (tcc/column datetimes)
          result (quarter col)]
      (is (= [1 2 3 4] (vec result)))))

  (testing "quarter boundaries are correct"
    ;; Verify exact boundaries: month 1,2,3->Q1, 4,5,6->Q2, etc.
    (let [dates [(parse "1970-03-31")  ; last day of Q1
                 (parse "1970-04-01")  ; first day of Q2
                 (parse "1970-06-30")  ; last day of Q2
                 (parse "1970-07-01")  ; first day of Q3
                 (parse "1970-09-30")  ; last day of Q3
                 (parse "1970-10-01")] ; first day of Q4
          col (tcc/column dates)
          result (quarter col)]
      (is (= [1 2 2 3 3 4] (vec result))))))

(deftest field-extractors-return-columns-test
  (testing "all field extractors return column objects"
    (let [dates [(parse "1970-01-01")]
          col (tcc/column dates)]
      (is (tcc/column? (year col)))
      (is (tcc/column? (month col)))
      (is (tcc/column? (day col)))
      (is (tcc/column? (day-of-week col)))
      (is (tcc/column? (day-of-year col)))
      (is (tcc/column? (week-of-year col)))
      (is (tcc/column? (quarter col)))))

  (testing "field extractors with time components return column objects"
    (let [datetimes [(LocalDateTime/of 1970 1 1 12 30 45)]
          col (tcc/column datetimes)]
      (is (tcc/column? (hour col)))
      (is (tcc/column? (minute col)))
      (is (tcc/column? (get-second col))))))
