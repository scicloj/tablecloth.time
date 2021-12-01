(ns tablecloth.time.api.adjust-frequency-test
  (:require [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tablecloth.api :refer [dataset grouped? column-names]]
            [tablecloth.time.api.converters :refer [->minutes]]
            [tablecloth.time.utils.datatypes :refer [get-datatype]]
            [tablecloth.time.api.adjust-frequency :refer [adjust-frequency]]
            [clojure.test :refer [testing deftest is]]))

(defn time-index [start-inst n tf]
  (plus-temporal-amount start-inst (range n) tf))

(defn get-test-ds [start-time num-rows temporal-unit]
  (dataset {:idx (time-index start-time num-rows temporal-unit)
            :key-a (reduce into [] (take (/ num-rows 2) (repeatedly #(vector "FOO" "BAR"))))
            :key-b (reduce into [] (take (/ num-rows 2) (repeatedly #(vector "FOO" "BAR"))))
            :value (take num-rows (repeatedly #(rand 200)))}))

(deftest test-adjust-frequency
  (let [ds (get-test-ds #time/instant "1970-01-01T00:00:00.000Z"
                        (* 2 60)
                        :seconds)]

    (testing "it returns a grouped dataset"
      (let [result (adjust-frequency ds ->minutes)]
        (is (= :dataset (get-datatype result)))
        (is (-> result grouped? not))))

    (testing "ungroup? option"
      (let [result (adjust-frequency ds ->minutes {:ungroup? false})]
        (is (-> result grouped?))))

    ;; No support for this now b/c we aren't doing bookeeping on the :index meta data yet
    (testing ":rename-index-to option"
      (let [result  (adjust-frequency ds ->minutes {:rename-index-to :minutes})]
        (is (some #{:minutes} (column-names result)))))
    ))
