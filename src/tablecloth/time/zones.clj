(ns tablecloth.time.zones
  "Utilities for discovering and working with time zones."
  (:require [clojure.string :as str])
  (:import [java.time ZoneId ZonedDateTime]
           [java.time.format TextStyle]
           [java.util Locale]))

(set! *warn-on-reflection* true)

(def common-time-zones
  "Commonly used time zones (recommended over deprecated 3-letter codes)."
  ["UTC"
   "America/New_York"    ; US Eastern
   "America/Chicago"     ; US Central
   "America/Denver"      ; US Mountain
   "America/Los_Angeles" ; US Pacific
   "Europe/London"
   "Europe/Paris"
   "Europe/Berlin"
   "Asia/Tokyo"
   "Asia/Shanghai"
   "Australia/Sydney"])

(defn available-time-zones
  "Return all available time zone IDs as a sorted vector."
  []
  (vec (sort (ZoneId/getAvailableZoneIds))))

(defn find-time-zones
  "Search for time zones containing the given string (case-insensitive).
  Spaces in the query are replaced with underscores to match zone ID format.

  Examples:
    (find-time-zones \"new york\")  ; => [\"America/New_York\"]
    (find-time-zones \"berlin\")    ; => [\"Europe/Berlin\"]
    (find-time-zones \"america\")   ; => [\"America/Chicago\" \"America/Denver\" ...]"
  [query]
  (let [q (-> query
              str/lower-case
              (str/replace #"\s+" "_"))]
    (->> (available-time-zones)
         (filter #(str/includes? (str/lower-case %) q))
         vec)))

(defn time-zone-info
  "Return helpful information about a time zone.

  Returns a map with:
    :id - the time zone ID
    :offset - current UTC offset (e.g. \"-05:00\")
    :dst? - whether DST is currently in effect
    :display-name - human-readable name

  Example:
    (time-zone-info \"America/New_York\")
    ; => {:id \"America/New_York\"
    ;     :offset \"-05:00\"
    ;     :dst? false
    ;     :display-name \"Eastern Standard Time\"}"
  [zone-str]
  (let [^ZoneId zone (ZoneId/of zone-str)
        ^ZonedDateTime now (ZonedDateTime/now zone)
        offset (.getOffset now)]
    {:id (.getId zone)
     :offset (str offset)
     :dst? (not= (.getRules zone)
                 (.getRules (ZoneId/of (str offset))))
     :display-name (.getDisplayName zone TextStyle/FULL Locale/US)}))
