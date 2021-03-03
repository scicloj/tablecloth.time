(ns tablecloth.time.api.rolling-window-test
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as tidx]
            [tablecloth.time.api.rolling-window :refer [rolling-window]]
            [midje.sweet :as sweet]))

(sweet/facts "rolling window dataset validations"
  (let [count 10
        len 3
        ds (-> (tablecloth/dataset [[:idx (take count (range))]
                                    [:values (map #(* 10 %) (take count (range)))]])
               (tidx/index-by :idx))
        rw (rolling-window ds len)]
    
    (sweet/fact "compare dataset sizes"
      (tablecloth/row-count rw) => count)))


