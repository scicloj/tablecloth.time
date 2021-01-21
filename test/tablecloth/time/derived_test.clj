(ns tablecloth.time.derived-test
  (:require [clojure.test :refer [deftest is]]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.derived :as derived]))

(deftest derived-test
  (let [ds (tablecloth/dataset {:x [1 2 3]
                                :y [4 5 6]})
        ds-with-derived (derived/add-derived ds
                                             [:x]
                                             :id1
                                             9999)]
    (is (-> ds-with-derived
            (tablecloth/select-rows [0 2])
            (derived/valid? :id1)
            not))
    (is (-> ds-with-derived
            (tablecloth/add-or-replace-column :x 9)
            (derived/valid? :id1)
            not))
    (is (-> ds-with-derived
            (tablecloth/add-or-replace-column :z 9)
            (derived/valid? :id1)))))


