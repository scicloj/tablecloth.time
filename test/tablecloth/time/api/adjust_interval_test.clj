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
                       :symbol "MSFT"
                       :price (take num-rows (repeatedly #(rand 200)))}))

;; (fact "`adjust-interval` returns a grouped dataset"
;;       (let [result (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z" (* 2 60) :seconds)
;;                        (adjust-interval :idx [:symbol] converters/->minutes :minutes))]
;;         (tablecloth/grouped? result) => false))

;; (fact "it passes through column specified as `keys`"
;;       (let [result (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z" (* 2 60) :seconds)
;;                        (adjust-interval :idx [:symbol] converters/->minutes :minutes))]
;;         (tablecloth/column-names result) => (contains [:symbol])))

(deftest test-adjust-internval
  (testing "it returns a grouped dataset"
    (is (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z" (* 2 60) :seconds)
            (adjust-interval :idx [:symbol] converters/->minutes :minutes)
            (tablecloth/grouped?))))

  (testing "it passes through columns specified as `keys`"
    (let [result (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z" (* 2 60) :seconds)
                     (adjust-interval :idx [:symbol] converters/->minutes :minutes))]
      (is (some #{:symbol} (tablecloth/column-names result))))))
