(ns tablecloth.time.utils.binary-search
  (:import [java.util Collections]))

;; Collections/binarySearch encodes the insertion point using
;; the forumla `-(insertion-point) - 1`. This fn reverses that.
;; This gives us the place where the searched for value should
;; be inserted.
(defn ->insertion-point [x]
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


