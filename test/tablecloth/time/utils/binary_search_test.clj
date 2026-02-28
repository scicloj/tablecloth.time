(ns tablecloth.time.utils.binary-search-test
  (:require [clojure.test :refer [deftest testing is]]
            [tablecloth.api :as tc]
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

(deftest test-ensure-time-column
  (testing "throws when column missing"
    (let [ds (tc/dataset {:a [1 2 3]})]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Time column not found"
           (bs/ensure-time-column ds :ts {})))))

  (testing "returns ds and column when already sorted"
    (let [ds (tc/dataset {:ts [1 2 3]
                          :x  [10 20 30]})
          {:keys [ds time-col time-col-series sorted?]}
          (bs/ensure-time-column ds :ts {})]
      (is (= ds ds))
      (is (= :ts time-col))
      (is (= [1 2 3] (vec time-col-series)))
      (is sorted?)))

  (testing "auto-sorts when unsorted and sort? true"
    (let [ds (tc/dataset {:ts [3 1 2]
                          :x  [30 10 20]})
          {:keys [ds time-col time-col-series sorted?]}
          (bs/ensure-time-column ds :ts {:sorted? false :sort? true})]
      (is (= :ts time-col))
      (is sorted?)
      (is (= [1 2 3] (vec time-col-series)))
      ;; Make sure the whole dataset was sorted, not just the column
      (is (= [10 20 30] (vec (tc/column ds :x))))))

  (testing "does not sort when sort? false"
    (let [ds (tc/dataset {:ts [3 1 2]
                          :x  [30 10 20]})
          {:keys [ds time-col time-col-series sorted?]}
          (bs/ensure-time-column ds :ts {:sorted? false :sort? false})]
      (is (= :ts time-col))
      (is (not sorted?))
      (is (= [3 1 2] (vec time-col-series)))
      (is (= [30 10 20] (vec (tc/column ds :x))))))

  (testing "respects sorted? hint"
    (let [ds (tc/dataset {:ts [3 2 1]
                          :x  [30 20 10]})
          {:keys [ds time-col time-col-series sorted?]}
          (bs/ensure-time-column ds :ts {:sorted? true :sort? true})]
      ;; We trust the caller and do not resort
      (is (= :ts time-col))
      (is sorted?)
      (is (= [3 2 1] (vec time-col-series)))
      (is (= [30 20 10] (vec (tc/column ds :x)))))))

(deftest test-find-upper-bound
  (testing "empty arr"
    (is (= -1 (bs/find-upper-bound [] 1)))
    (is (= -1 (bs/find-upper-bound [] -10))))
  (testing "single element in arr"
    (is (= 0 (bs/find-upper-bound [1] 1)))
    (is (= 0 (bs/find-upper-bound [1] 10))))
  (testing "target before all elements"
    (is (= -1 (bs/find-upper-bound [10 20 30 40] -10))))
  (testing "exact match on first element"
    (is (= 0 (bs/find-upper-bound [10 20 30 40] 10))))
  (testing "exact match on last element"
    (is (= 3 (bs/find-upper-bound [10 20 30 40] 40))))
  (testing "target after all elements"
    (is (= 3 (bs/find-upper-bound [10 20 30 40] 50))))
  (testing "target between elements"
    (is (= 2 (bs/find-upper-bound [10 20 30 40] 35))))
  (testing "array with duplicates"
    (is (= 3 (bs/find-upper-bound [10 20 20 20 30 40] 20)))
    (is (= 3 (bs/find-upper-bound [10 20 20 20 30 30 40] 22)))
    (is (= 3 (bs/find-upper-bound [10 20 20 20] 20)))
    (is (= 3 (bs/find-upper-bound [10 10 10 10] 10)))))

(deftest test-find-lower-bound
  (testing "empty and single element"
    (is (= 0 (bs/find-lower-bound [] 1)))
    (is (= 0 (bs/find-lower-bound [] -10)))
    (is (= 0 (bs/find-lower-bound [1] 1)))
    (is (= 1 (bs/find-lower-bound [1] 10)))
    (is (= 0 (bs/find-lower-bound [1] -10))))

  (testing "target before all elements"
    (is (= 0 (bs/find-lower-bound [10 20 30] 5))))

  (testing "exact match on first element"
    (is (= 0 (bs/find-lower-bound [10 20 30 40] 10))))

  (testing "exact match on last element"
    (is (= 3 (bs/find-lower-bound [10 20 30 40] 40))))

  (testing "exact match in middle"
    (is (= 1 (bs/find-lower-bound [10 20 30] 20))))

  (testing "target after all elements"
    (is (= 4 (bs/find-lower-bound [10 20 30 40] 50))))

  (testing "target between values"
    (is (= 2 (bs/find-lower-bound [10 20 30 40] 25))))

  (testing "duplicates - finds first occurrence"
    (is (= 1 (bs/find-lower-bound [10 20 20 20 30 40] 20)))
    (is (= 0 (bs/find-lower-bound [10 10 10 20 30] 10)))
    (is (= 2 (bs/find-lower-bound [10 20 30 30 30] 30))))

  (testing "all duplicates"
    (is (= 0 (bs/find-lower-bound [5 5 5 5] 5))))

  (testing "target between duplicates"
    (is (= 3 (bs/find-lower-bound [10 10 10 20 20 20] 15)))))
