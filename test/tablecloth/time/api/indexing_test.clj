(ns tablecloth.time.api.indexing-test
  (:require [clojure.test :refer [deftest is testing]]
            [tablecloth.time.time-literals]
            [tablecloth.time.api.indexing :refer [index-by rename-index]]
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

(deftest rename-index-test
  (let [ds (dataset {:x [#time/date "1970-01-01"
                         #time/date "1970-01-02"
                         #time/date "1970-01-03"]
                     :y [4 5 6]})]
    (testing "with index set explicitly"
      (let [renamed-ds (-> ds
                           (index-by :x)
                           (rename-index :z))]
        (is (= [:z]
               (-> renamed-ds
                   meta
                   (get-in [:validatable :index :column-names]))))))
    (testing "with index detected automatically"
      (let [renamed-ds (rename-index ds :z)]
        (-> renamed-ds
            :z
            nil?
            not)))))


