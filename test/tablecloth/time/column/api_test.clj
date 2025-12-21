(ns tablecloth.time.column.api-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.api.parse :refer [parse]]
            [tablecloth.time.column.api :refer [convert-time]])
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
