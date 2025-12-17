(ns tablecloth.time.api.parse
  (:import [java.time LocalDate LocalDateTime OffsetDateTime ZonedDateTime Instant]
           [java.time.format DateTimeFormatter ResolverStyle]))

(set! *warn-on-reflection* true)

(def ^:private ^{:doc "Cache of compiled DateTimeFormatter by a key map."}
  formatter-cache (atom {}))

(defn ^DateTimeFormatter resolve-formatter
  "Resolve a DateTimeFormatter from either:
   - `{:formatter f}` where f is a DateTimeFormatter (returned as-is), or
   - `{:format pattern}` where pattern is a java.time pattern string.
   Optional `:resolver-style` ∈ #{:strict :smart :lenient}. If absent, we leave the
   DateTimeFormatter's default (SMART) as-is."
  [{:keys [^DateTimeFormatter formatter format resolver-style]
    :as _opts}]
  (cond
    formatter formatter
    format (let [k {:format format :resolver-style resolver-style}
                 cached (get @formatter-cache k)]
             (if cached
               ^DateTimeFormatter cached
               (let [base (DateTimeFormatter/ofPattern ^String format)
                     ^DateTimeFormatter fmt (if resolver-style
                                              (.withResolverStyle base (case resolver-style
                                                                         :smart ResolverStyle/SMART
                                                                         :lenient ResolverStyle/LENIENT
                                                                         ResolverStyle/STRICT))
                                              base)]
                 (swap! formatter-cache assoc k fmt)
                 fmt)))
    :else nil))

(defn parse-with-formatter
  "Parse `s` with a DateTimeFormatter and return the first successfully
  materialized temporal among: OffsetDateTime, ZonedDateTime, LocalDateTime, LocalDate."
  ^Object [^CharSequence s ^DateTimeFormatter fmt]
  (let [s (str s)]
    (or (try (OffsetDateTime/parse s fmt) (catch Exception _ nil))
        (try (ZonedDateTime/parse s fmt) (catch Exception _ nil))
        (try (LocalDateTime/parse s fmt) (catch Exception _ nil))
        (try (LocalDate/parse s fmt) (catch Exception _ nil))
        (throw (ex-info "Failed to parse with explicit format"
                        {:type ::parse-failure
                         :input s
                         :format (str fmt)})))))

(defn parse
  "Parse a string `s` into a java.time value. For epoch millis, prefer
  `parse->millis`.

  Arities:
  - (parse s)           ; ISO-8601 only (strict default for these built-ins)
  - (parse s opts)

  opts map:
  - :format or :formatter — required for non-ISO inputs
  - :resolver-style — :strict | :smart | :lenient (only used with :format)
  - :zone — ZoneId or zone string for resolving LocalDate/LocalDateTime later
            (not needed if the parsed value has an offset/zone)

  Behavior:
  - Without :format/:formatter, attempts standard ISO parsers in order:
    Instant → OffsetDateTime → ZonedDateTime → LocalDateTime → LocalDate.
  - With :format/:formatter, tries the same order with the given formatter.
  - LocalTime by itself is not supported (throws)."
  ([s]
   (let [^String s s]
     (or (try (Instant/parse s) (catch Exception _ nil))
         (or (try (OffsetDateTime/parse s) (catch Exception _ nil))
             (try (ZonedDateTime/parse s) (catch Exception _ nil)))
         (try (LocalDateTime/parse s) (catch Exception _ nil))
         (try (LocalDate/parse s) (catch Exception _ nil))
         (throw (ex-info "Unparseable time string (expected ISO-8601); try providing :format"
                         {:type ::unparseable-iso :input s})))))
  ([s opts]
   (if-let [fmt (resolve-formatter opts)]
     (parse-with-formatter s fmt)
     (parse s))))
