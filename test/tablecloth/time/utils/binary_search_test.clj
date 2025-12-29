(ns tablecloth.time.utils.binary-search-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.time.utils.binary-search :as bs]))

(deftest test-is-sorted?
  (testing "empty and single element"
    (is (true? (bs/is-sorted? [])))
    (is (true? (bs/is-sorted? [1]))))
  
  (testing "sorted sequences"
    (is (true? (bs/is-sorted? [1 2 3 4 5])))
    (is (true? (bs/is-sorted? [1 2])))
    (is (true? (bs/is-sorted? [-5 -2 0 1 3]))))
  
  (testing "sorted with duplicates"
    (is (true? (bs/is-sorted? [1 2 2 3])))
    (is (true? (bs/is-sorted? [1 1 1 1])))
    (is (true? (bs/is-sorted? [1 2 2 2 3 3 4]))))
  
  (testing "unsorted sequences"
    (is (false? (bs/is-sorted? [1 3 2 4])))
    (is (false? (bs/is-sorted? [2 1])))
    (is (false? (bs/is-sorted? [1 2 3 5 4])))
    (is (false? (bs/is-sorted? [5 4 3 2 1]))))
  
  (testing "works with long sequences"
    (is (true? (bs/is-sorted? (vec (range 1000)))))
    (is (false? (bs/is-sorted? (vec (reverse (range 1000))))))
    (is (true? (bs/is-sorted? (vec (repeat 100 5)))))))
