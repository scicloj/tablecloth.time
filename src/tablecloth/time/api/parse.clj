(ns tablecloth.time.api.parse
  (:import [java.time LocalTime Instant OffsetDateTime ZonedDateTime
            LocalDateTime LocalDate])
  (:require [tablecloth.time.protocols.parseable :as parseable-proto]
            [tablecloth.time.api :refer [convert-time]]
            [tech.v3.datatype.datetime :as tddt]))

(defn parse-int [x]
  (Integer/parseInt x))

(extend-protocol parseable-proto/Parseable
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
      ;; #"\d{4}-\d{2}"
      ;; :>> (fn [s] (cljc.java-time.year-month/parse s))
      ;; #"\d{4}"
      ;; :>> (fn [s] (cljc.java-time.year/parse s))
      ;; (throw (ex-info "Unparseable time string" {:input s}))
      )))

(defn string->time
  "Given an identifiable time string or a datetime, returns the correct
  datetime object. Optionally, you can specify a target type to also
  convert to a different type in one step.

  TODO: How do we define what an 'identifiable' string means?"
  ([str]
   (parseable-proto/parse str))
  ([str datetime-type]
   (convert-time (parseable-proto/parse str) datetime-type)))
