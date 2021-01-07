(ns tablecloth.time.operations
  (:require [tablecloth.time.index :refer [get-index-meta]]
            [tablecloth.api :as tablecloth]
            [tech.v3.datatype.errors :as errors]
            [tick.alpha.api :as t]))

(defn get-slice [dataset from to]
  (let [index (get-index-meta dataset)
        row-numbers (if (not index)
                      (throw (Exception. "Dataset has no index specified."))
                      (-> index (.subMap from true to true) (.values)))]
    (tablecloth/select-rows dataset row-numbers)))

(defn slice-by-year [dataset from to]
  (let [from-year (t/year from)
        to-year (t/year to)]
    (get-slice dataset from-year to-year)))

(defn slice-by-date [dataset from to]
  (let [from-date (t/date from)
        to-date (t/date to)]
    (get-slice dataset from-date to-date)))


