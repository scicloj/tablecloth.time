(ns tablecloth.time.utils.indexing-test
  (:require [clojure.test :refer [deftest is testing]]
            [tablecloth.api :refer [dataset rename-columns add-column]]
            [tablecloth.time.api :refer [index-by]]
            [tablecloth.time.utils.indexing :as idx-utils]))

(deftest index-column-name-test
  (testing "manual index specified by `index-by`"
    (let [ds (dataset {:x [1 2 3]
                       :y [4 5 6]})]
      (is (= :x
             (-> ds
                 (index-by :x)
                 (idx-utils/index-column-name))))
      (is (= nil
             (-> ds
                 (index-by :x)
                 (rename-columns {:x :z}) ;; invalidates index
                 (idx-utils/index-column-name))))))

  (testing "when there is only one time column"
    (let [ds (dataset {:x [#time/date "1970-01-01"
                           #time/date "1970-01-02"
                           #time/date "1970-01-03"]
                       :y [4 5 6]})]
      (is (= :x
             (idx-utils/index-column-name ds)))
      (is (= :z
             (-> ds
                 (rename-columns {:x :z})
                 (idx-utils/index-column-name))))))

  (testing "when more than one time column and no index metadata"
    (let [ds (dataset {:x [#time/date "1970-01-01"
                           #time/date "1970-01-02"
                           #time/date "1970-01-03"]
                       :y [4 5 6]})]
      (is (= nil

             (-> ds
                 (add-column :z [#time/date "1970-01-01"
                                 #time/date "1970-01-02"
                                 #time/date "1970-01-03"])
                 (idx-utils/index-column-name)))))))
