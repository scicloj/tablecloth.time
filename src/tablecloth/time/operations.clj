(ns tablecloth.time.operations
  (:require [tablecloth.time.index :refer [get-index-meta]]
            [tablecloth.api :as tablecloth]
            [tech.v3.datatype.errors :as errors]
            [tick.alpha.api :as t]))

;; (slice "2010-01-01" "2015-01-01")
;; (slice "2015" "2017")

(defn get-slice [dataset from to]
  (let [index (get-index-meta dataset)
        row-numbers (if (not index)
                      (throw (Exception. "Dataset has no index specified."))
                      (-> index (.subMap from true to true) (.values)))]
    (tablecloth/select-rows dataset row-numbers)))

(defn slice-by-date [dataset from to]
  (let [from-date (t/date from)
        to-date (t/date to)]
    (get-slice dataset from-date to-date)))

(defn slice-by-year
  ([dataset from] (slice-by-year from ))
  ([dataset from to]
   (let [from-year (t/date (str from "-01-01"))
         to-year (t/date (str to "-01-01"))]
     (get-slice dataset from-year to-year))))

;; LocalDate date = LocalDate.now();
;; DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd");
;; String text = date.format(formatter);
;; LocalDate parsedDate = LocalDate.parse(text, formatter);


