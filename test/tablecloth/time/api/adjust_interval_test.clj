(ns tablecloth.time.api.adjust-interval-test
  (:require [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api.conversion :as converters]
            [tablecloth.time.api :refer [adjust-interval]]
            [midje.sweet :refer [fact contains]]
            [clojure.test :refer [testing deftest is]]))

(defn time-series [start-inst n tf]
  (plus-temporal-amount start-inst (range n) tf))

(defn get-test-ds [start-time num-rows temporal-unit]
  (tablecloth/dataset {:idx (time-series start-time num-rows temporal-unit)
                       :key-a "FOO"
                       :key-b "BAR"
                       :value (take num-rows (repeatedly #(rand 200)))}))

(deftest test-adjust-internval
  (let [test-dataset (get-test-ds #time/instant "1970-01-01T00:00:00.000Z"
                                  (* 2 60)
                                  :seconds)
        result (-> test-dataset (adjust-interval :idx
                                                 [:key-a, :key-b]
                                                 converters/->minutes
                                                 :minutes))]

    (testing "it returns a grouped dataset"
      (is (-> result (tablecloth/grouped?))))

    (testing "it passes through columns specified as `keys`"
      (is (some #{:key-a :key-b} (tablecloth/column-names result))))

    (testing "it creates a new time column specified by `new-column-name`"
      (is (some #{:minutes} (tablecloth/column-names result))))))
