(ns tablecloth.time.api.rolling-window-test
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :refer [index-by]]
            [tablecloth.time.api.rolling-window :refer [rolling-window]]
            [midje.sweet :as sweet]))

;; TODO: add tests to validate specific row behaviors

(sweet/facts "rolling window dataset validations"
  (let [count 10
        len 3
        ds (-> (tablecloth/dataset [[:idx (take count (range))]
                                    [:values (map #(* 10 %) (take count (range)))]])
               (index-by :idx))
        rw (rolling-window ds :idx len)]
    
    (sweet/fact "compare dataset sizes"
      (tablecloth/row-count rw) => count)))


(sweet/facts "rolling window dataset validations of time based indices"
  (let [len 3
        ds (-> (tablecloth/dataset {:A [#time/date "1970-01-01"
                                        #time/date "1970-01-02"
                                        #time/date "1970-01-03"
                                        #time/date "1970-01-04"
                                        #time/date "1970-01-05"
                                        #time/date "1970-01-06"
                                        #time/date "1970-01-07"
                                        #time/date "1970-01-08"
                                        #time/date "1970-01-09"
                                        #time/date "1970-01-10"
                                        #time/date "1970-01-11"
                                        #time/date "1970-01-12"]
                                    :B [1 2 3 4 5 6 7 8 9 10 11 12]})
               (index-by :A))
        rw (rolling-window ds :A len)]

    (sweet/fact "check dataset size"
      (tablecloth/row-count rw) => 12)))
