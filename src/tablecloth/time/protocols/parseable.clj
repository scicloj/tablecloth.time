(ns tablecloth.time.protocols.parseable
  (:import [java.time LocalTime Instant OffsetDateTime ZonedDateTime
            LocalDateTime LocalDate Year YearMonth]
           [org.threeten.extra YearQuarter]))

(defprotocol Parseable
  (parse [str] "Parse string to datetime."))

(defn- parse-int [x]
  (Integer/parseInt x))

(extend-protocol Parseable
  String
  (parse [str]
    (condp re-matches str
      #"(\d{1,2})"
      :>> (fn [[_ h]] (LocalTime/of (parse-int h) 0))
      #"\d{2}:\d{2}\S*"
      :>> (fn [s] (LocalTime/parse s))
      #"(\d{1,2}):(\d{2})"
      :>> (fn [[_ h m]] (LocalTime/of (parse-int h) (parse-int m)))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?Z"
      :>> (fn [s] (Instant/parse s))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?[+-]\d{2}:\d{2}"
      :>> (fn [s] (OffsetDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,9})?(?:[+-]\d{2}:\d{2}|Z)\[\w+/\w+\]"
      :>> (fn [s] (ZonedDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}T\S*"
      :>> (fn [s] (LocalDateTime/parse s))
      #"\d{4}-\d{2}-\d{2}"
      :>> (fn [s] (LocalDate/parse s))
      #"\d{4}-\d{2}"
      :>> (fn [s] (YearMonth/parse s))
      #"\d{4}-Q\d{1}"
      :>> (fn [s] (YearQuarter/parse s))
      #"\d{4}"
      :>> (fn [s] (Year/parse s))
      (throw (ex-info "Unparseable time string" {:input str})))))
