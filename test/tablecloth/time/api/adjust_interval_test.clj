(ns tablecloth.time.api.adjust-interval-test
  (:require [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tablecloth.api :refer [dataset grouped? column-names]]
            [tablecloth.time.api.converters :refer [->minutes]]
            [tablecloth.time.api :refer [adjust-interval]]
            [clojure.test :refer [testing deftest is]]))

(defn time-index [start-inst n tf]
  (plus-temporal-amount start-inst (range n) tf))

(defn get-test-ds [start-time num-rows temporal-unit]
  (dataset {:idx (time-index start-time num-rows temporal-unit)
            :key-a (reduce into [] (take (/ num-rows 2) (repeatedly #(vector "FOO" "BAR"))))
            :key-b (reduce into [] (take (/ num-rows 2) (repeatedly #(vector "FOO" "BAR"))))
            :value (take num-rows (repeatedly #(rand 200)))}))

(deftest test-adjust-interval
  (let [ds (get-test-ds #time/instant "1970-01-01T00:00:00.000Z"
                        (* 2 60)
                        :seconds)]

    (testing "it returns a grouped dataset"
      (let [result (-> ds (adjust-interval ->minutes))]
       (is (-> result (grouped?)))))

    (testing ":also-group-by option"
      (let [result (-> ds (adjust-interval ->minutes
                                           {:also-group-by [:key-a :key-b]}))]
        (is (some #{:key-a :key-b} (column-names result)))))

    (testing ":"
      (let [result (-> ds (adjust-interval ->minutes
                                           {:rename-index-to :minutes}))]
        (is (some #{:minutes} (column-names result)))))))


