(ns tablecloth.time.validatable-test
  (:require [clojure.test :refer [deftest is]]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.validatable :as validatable]))

(deftest validatable-test
  (let [ds (tablecloth/dataset {:x [1 2 3]
                                :y [4 5 6]})
        ds-with-validatable (validatable/add-validatable ds
                                                         [:x]
                                                         :id1
                                                         9999)]
    (is (-> ds ;; without validatable
            (validatable/valid? :id1)
            not))
    (is (-> ds-with-validatable
            (tablecloth/select-rows [0 2])
            (validatable/valid? :id1)
            not))
    (is (-> ds-with-validatable
            (tablecloth/add-or-replace-column :x 9)
            (validatable/valid? :id1)
            not))
    (is (-> ds-with-validatable
            (tablecloth/add-or-replace-column :z 9)
            (validatable/valid? :id1)
            not))))
