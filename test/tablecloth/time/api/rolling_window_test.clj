(ns tablecloth.time.api.rolling-window-test
  (:require [tablecloth.api :as tablecloth]
            [tablecloth.time.index :refer [index-by]]
            [tablecloth.time.api.rolling-window :refer [rolling-window]]
            [clojure.test :refer [deftest testing is]]))

;; TODO: add tests to validate specific row level behaviors

;; rolling window dataset validations
(deftest rolling-window-int-index-properties
  (let [count 10
        len 3
        ds (-> (tablecloth/dataset [[:idx (take count (range))]
                                    [:values (map #(* 10 %) (take count (range)))]])
               (index-by :idx))
        rw (rolling-window ds :idx len)]

    (testing
     "compare dataset sizes"
      (is (= (tablecloth/row-count rw) count)))))


