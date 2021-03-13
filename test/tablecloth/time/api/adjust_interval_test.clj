(ns tablecloth.time.api.adjust-interval-test
  (:use midje.sweet)
  (:require [tech.v3.datatype.datetime :refer [plus-temporal-amount]]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.api.conversion :as converters]
            [tablecloth.time.api :refer [adjust-interval]]))


(defn time-series [start-inst n tf]
  (plus-temporal-amount start-inst (range n) tf))

(defn get-test-ds [start-time num-rows temporal-unit]
  (tablecloth/dataset {:idx (time-series start-time num-rows temporal-unit)
                       :symbol "MSFT"
                       :price (take num-rows (repeatedly #(rand 200)))}))


(fact "returns a grouped dataset"
  (-> (get-test-ds #time/instant "1970-01-01T00:00:00.000Z"
                    (* 2 60)
                    :seconds)
      (adjust-interval :idx [:symbol] converters/->minutes :minutes)
      (tablecloth/grouped?)) => true) 
