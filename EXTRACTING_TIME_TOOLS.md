# Tablecloth.time Core Innovation Summary

Last reviewed: 2025-09-29

## The Key Innovation
The `tablecloth.time.api.converters` module implements a milliseconds-as-universal-pivot pattern for time conversions. Instead of dealing with Java's complex time class hierarchies, everything flows through a simple numeric representation:

```
Any Time Type → Milliseconds (long) → Any Other Time Type
```

This makes all conversions predictable, composable, and fast because the heavy lifting reduces to math on longs.

## Core Implementation Pattern
```clojure
(defn anytime->milliseconds [datetime] ...)
(defn milliseconds->anytime [millis target-type] ...)
(defn convert-to [datetime target-type]
  (-> datetime
      anytime->milliseconds
      (milliseconds->anytime target-type)))
```

## Why This Matters
1. Simplicity: Time becomes simple numeric data, not complex objects
2. Performance: Arithmetic on longs vs. object manipulation
3. Column-friendly: Designed to work well with tech.ml.dataset/dtype-next
4. Composability: All operations chain through milliseconds
5. No type confusion: One intermediate representation for everything

## Unique Features
- `down-to-nearest` (aliased as `->every`): Rounds time down to uniform intervals using modulo arithmetic on milliseconds.
- Period-end alignment helpers for time-series analytics: `->weeks-end`, `->months-end`, `->quarters-end`, `->years-end`.
- Extends dtype-next's type system to handle additional Java time classes (e.g., `:year`, `:year-month`).

## API at a glance
Short, representative examples using time literals.

```clojure
;; Converting across types (defaults to UTC)
(convert-to #time/instant "1970-01-01T00:00:00Z" :local-date)
;; => #time/date "1970-01-01"

(convert-to #time/date "1970-01-01" :instant)
;; => #time/instant "1970-01-01T00:00:00Z"

;; Rounding down to uniform intervals
((->every 15 :minutes) #time/instant "1970-01-01T00:07:13Z")
;; => #time/instant "1970-01-01T00:00:00Z"

(down-to-nearest 5 :days #time/date "1970-01-05")
;; => #time/date "1970-01-01"

;; Period ends
(->weeks-end   #time/instant "1970-01-01T00:00:00Z") ;; => #time/date "1970-01-04"
(->months-end  #time/date    "1970-01-01")           ;; => #time/date "1970-01-31"
(->quarters-end #time/date   "1970-01-01")           ;; => #time/date "1970-03-31"
(->years-end   #time/date    "1970-01-01")           ;; => #time/date "1970-12-31"

;; Parsing convenience
(string->time "1")       ;; => #time/time "01:00"
(string->time "9:30")    ;; => #time/time "09:30"
(string->time "1970-01-01") ;; => #time/date "1970-01-01"
```

## Assumptions and edge cases
- Default timezone: Unless a timezone is explicitly provided, conversions default to UTC via `dtdt/utc-zone-id`.
  - `anytime->milliseconds` and `milliseconds->anytime` have arities that accept a timezone.
  - `convert-to` currently does not accept a timezone argument and therefore uses UTC for both directions.
- Year type: `:year` currently converts to/from an integer year (e.g., `1970`), not a `java.time.Year` object. This preserves a simple data-first model. If desired, returning `Year/of` could be introduced behind a feature flag or alternate function.
- Interval rounding units: `milliseconds-in` supports `:milliseconds`, `:seconds`, `:minutes`, `:hours`, `:days`, and `:weeks`. Months/quarters are non-constant-length and thus intentionally not supported by `down-to-nearest`.
- Rounding semantics: `down-to-nearest` floors toward the previous boundary, including for negative epochs. For example, rounding `-3` seconds down to the nearest `5` seconds yields `-5` seconds.
- Type preservation: `down-to-nearest` returns the same logical time type as its input (e.g., `Instant` in → `Instant` out; `LocalDate` in → `LocalDate` out).

## Dependencies
- Core conversion utilities rely on `tech.v3.datatype.datetime` and dtype-next for type tagging and conversions.
- Period helpers for weeks and quarters use ThreeTen-Extra (`org.threeten.extra.YearWeek` and `YearQuarter`).
- A zero-dependency variant is feasible using direct `java.time` interop plus a minimal parsing layer; dtype-next integration can be supplied as an optional adapter.

## Separation Potential
This can be extracted into a standalone library with:
- Minimal dependencies: Potentially only `java.time` (JDK) + optional ThreeTen-Extra for period helpers
- Protocol-based design: Allow extensions for different time types without coupling to dtype-next
- Cross-platform story: Clojure, ClojureScript, and Babashka via consistent numeric pivot

## Competitive Advantage
Compared to wrappers like tick (thin `java.time` wrapper) or tempo (Temporal bridge):
- Data-first model: Treats time as data, not heavyweight objects
- Simpler arithmetic: Uniform operations on milliseconds
- Columnar friendliness: Optional adapter enables efficient dataset operations

## Current Status
The parent project has non-functional pieces due to removal of index functionality in `tech.ml.dataset`, but the time conversion approach is self-contained and remains valuable. It can be revived as a standalone library independent of index features.

## Next Steps
- Extract `converters` and related helpers into a standalone "time-pivot" core library.
- Provide an optional adapter for dtype-next/tech.ml.dataset and for ThreeTen-Extra-based period helpers.
- Decide on `:year` representation (keep integer vs. return `java.time.Year`) and document the choice clearly.
- Consider adding a timezone-aware arity for `convert-to`, mirroring the lower-level functions.
- Add explicit docs and doctests for supported interval units and rounding behavior.
