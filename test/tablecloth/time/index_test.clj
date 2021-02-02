(ns tablecloth.time.index-test
  (:require [clojure.test :refer [deftest is]]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as index]))

(deftest slice-index-result-types
  (let [ds (tablecloth/dataset {:x [1 2 3]
                                :y [4 5 6]})]
    (is (instance? java.util.AbstractMap$2
                   (-> ds
                       (index/index-by :x)
                       (index/slice-index 2 3 {:result-type :as-indexes}))))
    (is (instance? tech.v3.dataset.impl.dataset.Dataset
                   (-> ds
                       (index/index-by :x)
                       (index/slice-index 2 3 {:result-type :as-dataset}))))
    ;; default behavior
    (is (instance? tech.v3.dataset.impl.dataset.Dataset
                   (-> ds
                       (index/index-by :x)
                       (index/slice-index 2 3))))))
