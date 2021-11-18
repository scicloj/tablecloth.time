(ns tablecloth.time.api.time-components
  (:import [java.time Month LocalDateTime])
  (:require [tablecloth.time.api.converters :refer [->local-date-time]]))

(defn- extract-datetime-component-simple
  [extract-fn datetime]
  (-> datetime
      (->local-date-time)
      (extract-fn)))

;; Would be nice to type hint `time-component`. May need a macro for this.
(defn- extract-datetime-component-complex
  ([extract-fn datetime]
   (extract-datetime-component-complex
    extract-fn
    datetime
    {:as-number? false :as-class? false}))
  ([extract-fn datetime {:keys [as-number? as-class?]}]
   (let [^LocalDateTime ldt (->local-date-time datetime)
         time-component (extract-fn ldt)]
     (cond
       as-number? (.getValue time-component) ;; reflection warning
       as-class? time-component
       :else (.toString ^java.lang.Object time-component)))))

(def ^{:doc "Extracts year (as number) from any datetime."
       :argslist '([datetime])}
  year (partial extract-datetime-component-simple
                #(.getYear ^LocalDateTime %)))

(def ^{:doc "Extracts year (as number) from any datetime."
       :argslist '([datetime])}
  dayofyear (partial extract-datetime-component-simple
                     #(.getDayOfYear ^LocalDateTime %)))

(def ^{:doc "Extract month from any datetime."
       :argslist '([datetime] [datetime {:keys [as-number? as-class?]}])}
  month (partial
         extract-datetime-component-complex
         #(.getMonth ^LocalDateTime %)))

(def ^{:doc "Extracts the day of the month from any datetime."
       :argslist '([datetime])}
  dayofmonth (partial extract-datetime-component-simple
                      #(.getDayOfMonth ^LocalDateTime %)))

(def ^{:doc "Extract the day of week from any datetime."
       :argslist '([datetime] [datetime {:keys [as-number? as-class?]}])}
  dayofweek (partial
             extract-datetime-component-complex
             #(.getDayOfWeek ^LocalDateTime %)))

(def ^{:doc "Extracts the hour from any datetime."
       :arglists '([datetime])}
  hour (partial extract-datetime-component-simple
                #(.getHour ^LocalDateTime %)))

(def ^{:doc "Extracts the minute of hour (number) from any datetime."
       :arglists '([datetime])}
  minute (partial extract-datetime-component-simple
                  #(.getMinute ^LocalDateTime %)))

(def ^{:doc "Extracts the second of minute from any datetime."
       :arglists '([datetime])}
  secnd (partial extract-datetime-component-simple
                 #(.getSecond ^LocalDateTime %)))
