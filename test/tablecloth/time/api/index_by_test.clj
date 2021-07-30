(ns tablecloth.time.api.index-by-test
  (:require [clojure.test :refer [deftest is testing]]
            [tablecloth.time.api :refer [index-by]]
            [tablecloth.api :refer [dataset]]))

(deftest index-by-test
  (testing "index 'validatable'"
    (let [ds-with-index (-> (dataset {:x [1 2 3]
                                      :y [4 5 6]})
                            (index-by :x))]
      (is (contains? (meta ds-with-index)
                     :validatable))
      (is (contains? (:validatable (meta ds-with-index))
                     :index)))))


