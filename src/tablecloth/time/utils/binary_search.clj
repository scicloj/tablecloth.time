(ns tablecloth.time.utils.binary-search
  (:import [java.util Collections]) 
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.functional :as fun]
            [tablecloth.column.api :as tcc]))

(defn is-sorted?
  ([col]
   (is-sorted? col :ascending))
  ([col direction]
   (cond
     (or
      (empty? col)
      (= 1 (dtype/ecount col)))
     true
     (pos? (tcc/count-missing col))
     false
     :else
     (let [op (if (= direction :ascending) fun/>= fun/<=)
           shifted (fun/shift col -1)
           compared (op shifted col)]
       (= (dtype/ecount col)
          (fun/reduce-+ compared))))))

;; Collections/binarySearch encodes the insertion point using
;; the forumla `-(insertion-point) - 1`. This fn reverses that.
;; This gives us the place where the searched for value should
;; be inserted.
(defn- ->insertion-point [x]
  (- (inc x)))

(defn find-lower-bound [arr target]
  (let [result (Collections/binarySearch arr target)]
    (if (>= result 0)
      result  ; exact match
      (->insertion-point result))))  ; insertion point

(defn find-upper-bound [arr target]
  (let [result (Collections/binarySearch arr target)]
    (if (>= result 0)
      result  ; exact match
      (dec (->insertion-point result)))))  ; insertion point - 1

