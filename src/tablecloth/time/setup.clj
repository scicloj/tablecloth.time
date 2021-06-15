(ns tablecloth.time.setup
  (:import [java.util TreeMap]
           [org.threeten.extra YearQuarter])
  (:require [tech.v3.datatype.casting :refer [add-object-datatype!]]
            [tech.v3.dataset.impl.column-index-structure :as cindex]))

(add-object-datatype! :year java.time.Year true)
(add-object-datatype! :year-month java.time.YearMonth true)
(add-object-datatype! :year-quarter YearQuarter true)

(defn- do-build-index [data]
  (let [^java.util.Map idx-map (cindex/build-value-to-index-position-map data)]
    (TreeMap. idx-map)))

(defmethod cindex/make-index-structure :year
  [data _]
  (do-build-index data))

(defmethod cindex/make-index-structure :year-quarter
  [data _]
  (do-build-index data))

(defmethod cindex/make-index-structure :year-month
  [data _]
  (do-build-index data))
