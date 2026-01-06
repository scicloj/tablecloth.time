# tablecloth.time – development plan

_Last updated: 2024-12-28_

## 0. Current status and archived work

**Archived legacy code** (2024-12-28):

We have archived both source and tests for several defunct/legacy namespaces in `_archive/` (at project root):
- `adjust_frequency_test.clj` - old bucketing/resampling API
- `converters_test.clj` - old converter functions (now in `column.api`)
- `rolling_window_test.clj` - old rolling window API
- `slice_test.clj` - old slice/index-by API
- `time_components_test.clj` - old time field extractors (now in `column.api`)
- `validatable_test.clj` - dataset integrity checking utility

These are preserved as **reference material** for when we reimplement similar functionality in the new column-based architecture. They provide valuable test cases, edge cases, expected behavior semantics, and implementation patterns.

Both source and test files have been moved outside of `src/` and `test/` directories so they are not loaded or run. The main `tablecloth.time.api` namespace has been cleaned up to remove defunct symbol exports.

**Active work**:
- ✅ Column-level field extractors implemented in `tablecloth.column.api`
- ✅ Column-level `convert-time` for representation changes
- ✅ Dataset-level `slice` operation implemented in `tablecloth.time.api.slice`
- 🚧 Dataset-level operations (bucket, resample) to be reimplemented per the architecture below

## 1. Scope and direction

**Primary audience:** Scicloj / tablecloth users working with `tech.ml.dataset` datasets.

**Goal:** Make common time manipulations (slicing, bucketing, rolling, etc.) easy and expressive *in the dataset context*, while using dtype-next for performance.

### Design decisions

- Time functionality should live in **`tablecloth.time`**, not in a separate `gnomon` lib, because:
  - The main use cases are tablecloth + `tech.ml.dataset`, not general JVM apps.
  - `tablecloth.time` already has the right context: datasets, columns, and the `tablecloth.column.api`.
  - We can "lift" dtype-next datetime operations into the column API directly, similar to how functional ops were lifted from `tech.v3.datatype.functional`.
- The earlier `gnomon` work remains valuable as a **source of semantics and tests** (especially around the millis pivot and bucketing), but it does not need to be a public library right now.
  - If a clear, non-tablecloth use case appears later, we can reconsider a tiny scalar library.

---

## 2. Millis pivot – core mental model (from gnomon)

We keep the **millis pivot** idea but apply it at the **column** level.

Conceptually (gnomon, scalar version):

- There is a **single numeric axis**: epoch milliseconds.
- Conversions:
  - `to-millis`: `Instant` / `ZonedDateTime` / `OffsetDateTime` / `LocalDateTime` / `LocalDate` / `java.util.Date` / number → epoch millis.
  - `millis->anytime`: millis → specific `java.time` type (or keyword designator, e.g. `:instant`, `:local-date`).
  - `convert-to`: combines the two with explicit `:zone` semantics.
- Bucketing / rounding:
  - `milliseconds-in unit` (e.g. seconds, minutes, hours, days, weeks) gives a scalar factor.
  - `down-to-nearest interval unit` / `->every`:
    - For metric units (`:milliseconds`, `:seconds`, `:minutes`, `:hours`, `:days`, `:weeks`): floor using integer math in millis (`x - (mod x step)`).
    - For calendar units (`:months`, `:quarters`, `:years`): convert to `LocalDate`, apply calendar-aware floor functions (`floor-month`, `floor-quarter`, `floor-year`), then convert back.

In `tablecloth.time`, we adapt this to **columns**:

- Normalize time columns to epoch millis where needed (using dtype-next datetime operations).
- Do bucketing and other math in millis (or another numeric epoch space as appropriate).
- Convert back to logical datetime dtypes for result columns.

This keeps user-facing code thinking in terms of **time and columns**, not low-level numeric representations.

---

## 3. Time axis / index model (no dedicated Index type)

We do **not** reintroduce a pandas-style Index into `tech.ml.dataset`. Instead, we:

1. Treat an **index as just a column** whose values we regard as the coordinate system (usually time).
   - Example: `:received-date` is the time axis.

2. **No metadata-based axis marking**: Time operations explicitly take column name arguments, consistent with tablecloth's patterns:
   - `group-by`, `order-by`, `aggregate`, etc. all take explicit column selectors.
   - Time operations follow the same pattern: `(slice-by ds :time-col start end)`.
   - This keeps behavior clear and avoids "spooky action at a distance" from metadata.

3. **No `index-by` function**: Deferred unless a compelling use case emerges. Explicit column arguments are simpler and more flexible.

4. **Sortedness handling**:
   - Time slicing operations require sorted data for efficient binary search.
   - Default behavior: check sortedness (O(n)), use binary search if sorted, error with helpful message if not.
   - Optimization: `{:sorted? true}` option skips the sortedness check (for when user knows data is sorted).
   - We never silently reorder data - users must explicitly `(tc/order-by ds :time-col)` first.

### Harold's "index as closure" idea

From the Zulip `tech.ml.dataset.dev` discussion (summarized):

- For categorical indexing, Harold uses:

  ```clojure
  (defn obtain-index [ds colname]
    (let [m (ds/group-by-column->indexes ds colname)]
      (fn [v]
        (map (partial ds/row-at ds) (get m v)))))

  (def lookup (obtain-index ds :id))
  (lookup "a")  ; rows where :id == "a"
  ```

- This yields an **index-as-function**:
  - Built once (`group-by-column->indexes` is O(n)).
  - Lookup is O(1) using a hash map.
  - Rows are realized lazily.
  - No special Index type, just a map + closure.

For **time / ordered data**, the analogous concept is:

- Treat the **sorted time column itself** as the index.
- Use **binary search** over the sorted time column (always; no conditional logic).
- Binary search is fast even on small data (100 rows = ~7 comparisons) and provides consistent, predictable performance.
- For more complex windowing operations, use dtype-next's `variable-rolling-window-ranges`.

**Why binary search instead of tree structures?** (from Zulip discussion with Chris Nuernberger)

Chris Nuernberger (dtype-next author) explained:
> "You only need tree structures if you are adding values ad-hoc or removing them - usually with datasets we aren't adding/removing rows but rebuilding the index all at once. **Just sorting the dataset and using binary search will outperform most/all tree structures in this scenario as it is faster to sort than to construct trees. Binary search performs as well as any tree search for queries and range queries.**"

Harold validated this with real-world performance: **>1M rows/s** using `java.util.Collections/binarySearch` on a 1M row time series dataset.

Key insight: Datasets are typically **rebuilt wholesale** (loaded/reloaded), not incrementally modified. In this scenario:
- **Sorting is faster than constructing trees**
- **Binary search performs as well as tree search** for queries
- **Simpler implementation** with predictable behavior

See [`doc/zulip-indexing-discussion-summary.md`](../doc/zulip-indexing-discussion-summary.md) for full context.

In practice for tablecloth:

- No special Index type, no metadata tracking, no tree structures.
- Time operations take explicit column arguments and assume/verify sortedness.
- Binary search provides efficient O(log n) slicing over sorted time columns.

---

## 4. Column-level time primitives (in `tablecloth.time.column.api`)

We want a small, coherent set of **column primitives** that:

- Take a column-like thing (vector, dtype-next column/reader).
- Return a new column-like thing.
- Encode the millis-pivot and calendar semantics.

Sketch of desired primitives:

### 4.1. Conversions

```clojure
; Normalize a datetime-ish column to epoch millis
(->millis-col [col opts])

; Inverse: millis column -> datetime column of a target dtype
(millis->datetime-col [millis-col target-dtype opts])
``

- Internally, these should use:
  - `tech.v3.datatype.datetime/datetime->milliseconds`
  - `tech.v3.datatype.datetime/milliseconds->datetime`
  - and related dtype-next datetime operations.

### 4.2. Bucketing / rounding

```clojure
; Vectorized gnomon-style bucketing
(bucket-every-col [time-col interval unit opts])
```

Semantics:

- Metric units (`:milliseconds`, `:seconds`, `:minutes`, `:hours`, `:days`, `:weeks`):
  - Normalize to millis if necessary.
  - Compute `divisor = interval * (milliseconds-in unit)`.
  - For each element: `rounded = millis - (mod millis divisor)`.

- Calendar units (`:months`, `:quarters`, `:years`):
  - Normalize to a `LocalDate` column (via dtype-next conversion from millis).
  - Apply calendar-aware floors (`floor-month`, `floor-quarter`, `floor-year`) per element.
  - Convert back to the desired type (or to millis) based on the calling context.

These operations are **index-agnostic** in the sense that they are per-row transforms; they do not need ordering. They will be composed with index-aware dataset-level operations when needed.

### 4.3. Field extraction (✅ implemented)

We have implemented field extractor functions in `tablecloth.time.column.api`:

- **Basic calendar fields**: `year`, `month`, `day`, `hour`, `minute`, `get-second`
- **Derived calendar fields**: `day-of-week`, `day-of-year`, `week-of-year`, `quarter`

All functions:
- Wrap `dtdt-ops/long-temporal-field` for efficient vectorized extraction
- Handle `Instant` columns automatically (convert to `LocalDateTime` UTC for extraction)
- Work with `LocalDate`, `LocalDateTime`, `ZonedDateTime`, and `Instant` columns
- Return wrapped `tcc/column` objects

Comprehensive tests added covering all datetime types and edge cases.

### 4.4. Additional column operations to consider

Based on pandas dt accessor and R lubridate, here are additional column-level operations for future implementation:

#### 4.4.1. Rounding operations (HIGH PRIORITY)

We have `down-to-nearest` (floor) but are missing:

```clojure
(defn ceil-to-nearest [col interval unit opts] ...)  ; Round up to next boundary
(defn round-to-nearest [col interval unit opts] ...) ; Round to nearest boundary
```

**Rationale**: Pandas provides `.round`, `.floor`, and `.ceil` methods. Lubridate provides `ceiling_date()`, `floor_date()`, and `round_date()`. For ceil, it always returns the upper bound of the period; for round, it returns the nearest bound.

**Implementation notes**:
- `ceil-to-nearest`: floor + (if not already aligned, add one interval)
- `round-to-nearest`: floor, then check if distance to next boundary is less than half interval

#### 4.4.2. Temporal arithmetic (MEDIUM PRIORITY)

```clojure
(defn plus-time [col amount unit opts] ...)   ; Add time amounts
(defn minus-time [col amount unit opts] ...)  ; Subtract time amounts
(defn between [col1 col2 unit opts] ...)      ; Difference between two datetime columns
```

**Rationale**: dtype-next provides `plus-temporal-amount`, `minus-temporal-amount`, and `between`. Lubridate offers efficient arithmetic operations on dates. Essential for "shift by N days" or "time since previous event" operations.

**Implementation notes**:
- Wrap `dtdt-ops/plus-temporal-amount` and `dtdt-ops/minus-temporal-amount`
- Support same units as `down-to-nearest`: `:seconds`, `:minutes`, `:hours`, `:days`, `:weeks`, `:months`, `:years`
- `between` wraps `dtdt-ops/between` for column-wise time differences
- Handle zone semantics consistently with `convert-time`

#### 4.4.3. Boolean predicates (LOWER PRIORITY, but useful)

```clojure
(defn is-month-start? [col] ...)   ; True if first day of month
(defn is-month-end? [col] ...)     ; True if last day of month
(defn is-quarter-start? [col] ...) ; True if first day of quarter
(defn is-quarter-end? [col] ...)   ; True if last day of quarter
(defn is-year-start? [col] ...)    ; True if first day of year
(defn is-year-end? [col] ...)      ; True if last day of year
(defn is-leap-year? [col] ...)     ; True if year is leap year
(defn is-weekend? [col] ...)       ; True if Saturday or Sunday
```

**Rationale**: Pandas dt accessor provides `is_month_start`, `is_month_end`, etc. Lubridate's `leap_year()` tests for leap years. Useful for filtering and conditional logic in pipelines.

**Implementation notes**:
- Compare field extractors with boundary values
- `is-month-start?`: day == 1
- `is-month-end?`: compare with days-in-month (account for leap years)
- `is-leap-year?`: `(and (= 0 (mod year 4)) (or (!= 0 (mod year 100)) (= 0 (mod year 400))))`
- `is-weekend?`: `(or (= day-of-week 6) (= day-of-week 7))`

#### 4.4.4. Timezone operations (MEDIUM PRIORITY)

```clojure
(defn with-tz [col new-zone] ...)     ; Change display timezone (same instant)
(defn force-tz [col new-zone] ...)    ; Change zone metadata (different instant)
```

**Rationale**: Lubridate provides `with_tz()` (changes time zone display, same moment) and `force_tz()` (changes only zone metadata, describes new moment). Critical for multi-timezone datasets.

**Implementation notes**:
- `with-tz`: Convert to `ZonedDateTime` with new zone (instant unchanged)
- `force-tz`: Reinterpret clock time in new zone (instant changes)
- Only works with temporal types that have zone information

#### 4.4.5. Descriptive statistics (LOWER PRIORITY)

```clojure
(defn datetime-min [col] ...)         ; Minimum datetime value
(defn datetime-max [col] ...)         ; Maximum datetime value
(defn datetime-mean [col] ...)        ; Mean datetime value
(defn datetime-range [col] ...)       ; max - min as a duration
```

**Rationale**: dtype-next provides `millisecond-descriptive-statistics`. While these might be better as dataset-level aggregations, having column-level helpers is convenient.

**Implementation notes**:
- Wrap `dtdt-ops/millisecond-descriptive-statistics`
- Convert results back to appropriate datetime types
- Consider whether these belong here or in aggregation layer

#### 4.4.6. Normalization operations (QUICK WIN)

```clojure
(defn normalize-date [col opts] ...)  ; Set time component to 00:00:00
```

**Rationale**: Pandas `dt.normalize` always rounds time to midnight (00:00:00). Common operation for comparing dates ignoring time components.

**Implementation notes**:
- Convert to `LocalDate` and back to original temporal type
- Equivalent to `floor-to-day` but more explicit intent

#### 4.4.7. String formatting (NICE-TO-HAVE)

```clojure
(defn strftime [col format-string opts] ...)  ; Format as string
(defn day-name [col opts] ...)               ; "Monday", "Tuesday", etc.
(defn month-name [col opts] ...)             ; "January", "February", etc.
```

**Rationale**: Pandas `Series.dt.strftime` converts to string using specified format. Useful for labels and display.

**Implementation notes**:
- Use Java's `DateTimeFormatter` for `strftime`
- `day-name` and `month-name` can use built-in formatters
- Consider locale support for internationalization

#### 4.4.8. Implementation priorities

**Implement immediately**:
1. Rounding operations (`ceil-to-nearest`, `round-to-nearest`) - completes rounding trilogy
2. Temporal arithmetic (`plus-time`, `minus-time`, `between`) - enables time-delta operations

**Implement soon**:
3. Timezone operations (`with-tz`, `force-tz`) - critical for multi-timezone use cases
4. Normalization (`normalize-date`) - common operation, easy to implement

**Nice-to-have**:
5. Boolean predicates - useful for filtering
6. String formatting - display and export
7. Descriptive statistics - may fit better in aggregation layer

### 4.5. Column-level `convert-time` (current MVP)

We now have a first version of a column-level `convert-time` in
`tablecloth.time.column.api` with the following semantics:

- **Purpose**: convert between time *representations* at the column level:
  - temporal ↔ epoch (e.g. `:local-date` ↔ `:epoch-milliseconds`).
  - temporal ↔ temporal (via a millis pivot).
  - epoch ↔ epoch (via numeric scaling only).
- **Source/target classification**:
  - Use `dtype/elemwise-datatype` + `dtdt-base/classify-datatype` to classify
    the source and target dtypes into `:temporal`, `:epoch`, `:duration`,
    `:relative`, etc.
  - Only `[:temporal …]` and `[:epoch …]` combinations are supported; anything
    involving `:duration` or `:relative` throws a clear
    `::unsupported-time-conversion` `ExceptionInfo`.
- **Targets**:
  - Accept both keywords and Java time classes.
  - Normalize via a private `normalize-target` helper that:
    - handles a small synonym map (e.g. `:zdt` → `:zoned-date-time`,
      `:ldt` → `:local-date-time`).
    - uses `casting/object-class->datatype` for class targets.
- **Packed vs unpacked dtypes**:
  - We use `tech.v3.datatype.packing/unpack-datatype` in
    `calendar-local-type?` so that both logical and packed
    `LocalDate`/`LocalDateTime`/`LocalTime` types are treated as
    "calendar local" when deciding whether a zone is needed.
- **Zone semantics**:
  - The public `convert-time` default `:zone` is **UTC**, implemented via
    `coerce-zone-id` with `{:default (dtdt/utc-zone-id)}`.
  - Zone is only passed to dtype-next conversions when the temporal side is
    calendar-local (no inherent zone), or when doing temporal↔temporal via the
    millis pivot.
  - Epoch↔epoch conversions ignore zone entirely (they are pure numeric
    rescalings of an absolute UTC instant representation).
- **Epoch↔epoch workaround**:
  - We avoid going through datetime for epoch↔epoch conversions (to dodge a
    dtype-next bug around `:epoch-seconds` + zone) and instead scale via
    `epoch->microseconds` and `tech.v3.datatype.functional`.

We should keep `convert-time` focused on **representation changes** only; all
operations that conceptually involve *lengths* (durations/relatives) will have
separate APIs (e.g. `between`/`time-diff`, `convert-duration`).

---

## 5. Dataset-level helpers (sketch)

On top of column primitives, we can define dataset-level helpers that feel natural in tablecloth. All time operations take **explicit column arguments**, consistent with tablecloth's existing API patterns.

### 5.1. Slicing by time (✅ implemented)

Implemented in `tablecloth.time.api.slice/slice`:

```clojure
(slice [ds time-col start end opts])
```

Example:
```clojure
(slice ds :received-date "2022-01-01" "2022-12-31")
(slice ds :timestamp #time/date "2022-01-01" #time/date "2022-12-31")
(slice ds :timestamp 1704067200000 1704153599999 {:result-type :as-indices})
```

**Implementation details:**

- Parse `start`/`end` using `tablecloth.time.parse/parse` (ISO-8601 strings) or accept temporal types/epoch millis directly.
- Normalize the chosen time column to epoch milliseconds using `convert-time`.
- Check sortedness (O(n)) and auto-sort if needed (ascending order).
- Support both ascending and descending sorted data.
- Use custom binary search (`tablecloth.time.utils.binary-search`) to find `[start-idx end-idx]` range.
- Return dataset by default or indices with `{:result-type :as-indices}`.

**Sortedness semantics:**
- Default: check sortedness using `is-sorted?`, auto-sort in ascending order if needed.
- Supports both ascending and descending sorted data (detects direction automatically).
- Never errors on unsorted data—just sorts it transparently.

**Binary search strategy:**
- Always use binary search (no conditional logic or thresholds).
- Custom implementation in `tablecloth.time.utils.binary-search` for lower/upper bound finding.
- Fast even on small data: 100 rows = ~7 comparisons.
- Provides consistent, predictable performance.

**Comprehensive test coverage:**
- Ascending and descending sorted data
- String dates, time literals, and epoch milliseconds
- Multi-month ranges, single-row datasets, duplicate timestamps
- Edge cases: empty results, out-of-range queries, boundary matches
- Error handling: invalid date ranges, missing columns

### 5.2. Bucketing and rollups

**Design decision**: No dedicated `rollup-every` or `add-bucket-column` functions.

Following the **R/dplyr philosophy** of composable primitives, users can easily compose bucketing workflows using our column-level time functions with standard tablecloth operations:

```clojure
;; Bucket and aggregate
(-> ds
    (tc/add-column :bucket #(down-to-nearest (% :timestamp) 5 :minutes))
    (tc/group-by :bucket)
    (tc/aggregate {:count tc/row-count 
                   :avg-value #(dfn/mean (% :value))}))

;; Or use field extractors for natural calendar boundaries
(-> ds
    (tc/add-column :year #(year (% :timestamp)))
    (tc/add-column :month #(month (% :timestamp)))
    (tc/group-by [:year :month])
    (tc/aggregate {:total #(dfn/sum (% :sales))}))
```

This is:
- **Transparent**: you see exactly what's happening at each step
- **Flexible**: easy to customize (keep/drop bucket column, add filters, etc.)
- **Consistent**: uses standard tablecloth patterns users already know
- **Simple**: just 3 lines of straightforward code

A dedicated `rollup-every` function would be too thin to justify—unlike `slice`, which handles significant complexity (parsing, sorting, binary search), bucketing workflows are simple compositions of existing tools.

**Possible future consideration**: A `rollup-every` helper might be justified if user feedback shows this is a very common pattern and convenience is valued. But we'll start with composable primitives and add convenience functions only if needed.

---

## 6. Practical next steps

1. **Column API:** (✅ mostly complete)
   - ✅ Field extractors implemented in `tablecloth.time.column.api` (year, month, day, hour, minute, second, day-of-week, day-of-year, week-of-year, quarter)
   - ✅ `convert-time` for representation changes (temporal ↔ epoch)
   - 🚧 Still needed:
     - `bucket-every-col` / `align-time` / `floor-time` for rounding/bucketing operations
     - Additional rounding operations (`ceil-to-nearest`, `round-to-nearest`)
     - Temporal arithmetic (`plus-time`, `minus-time`, `between`)

2. **Binary search helper:** (✅ complete)
   - ✅ Custom implementation in `tablecloth.time.utils.binary-search`
   - ✅ Supports both lower-bound and upper-bound finding
   - ✅ Always uses binary search (no conditional logic)

3. **Dataset API:** (✅ slice complete, bucketing/resampling/interpolation todo)
   - ✅ `slice` implemented in `tablecloth.time.api.slice` with:
     - Explicit time column argument
     - Auto-sorting if unsorted (no error-on-unsorted)
     - Binary search for range finding
     - Comprehensive test coverage
   - 🚧 Still needed (downsampling/aggregation):
     - ~~`rollup-every`~~ **Deprioritized**: users can compose `down-to-nearest` + `tc/add-column` + `tc/group-by` + `tc/aggregate`
     - No dedicated dataset-level bucketing functions planned (composition is sufficient)
   - 🚧 Still needed (upsampling/interpolation):
     - `resample-to-regular-grid` for irregular → regular time series
     - Column primitives: `generate-time-range`, `interpolate-values`
     - Interpolation methods: `:ffill`, `:bfill`, `:linear`, `:nearest`, `:zero`

4. **Reuse from gnomon:** (🚧 in progress)
   - Port tests and semantics from the gnomon repo (especially for `down-to-nearest`, `->every`, and conversion behaviors) into `tablecloth.time` tests.
   - Use those semantics to guide the column-level and dataset-level implementations.

5. **De-prioritize gnomon as a standalone library** until/unless a clear general JVM use case emerges.

---

## 7. Rounding / aligning / resampling semantics (notes)

We clarified terminology and design layers around bucketing and resampling:

- **Aligning / flooring** (scalar or column):
  - The old scalar `down-to-nearest` from gnomon is conceptually a
    *time-floor/align* operation:
    - Given `n` and a unit (`:seconds`, `:minutes`, `:days`, etc.), map each
      timestamp to the **start of the nearest-lower interval** of that size.
  - At the column level this becomes a vectorized op (implemented with
    dtype-next) that is *index-agnostic*:

    ```clojure
    (down-to-nearest col 10 :seconds ...) ; or align/floor naming variant
    ```

- **Bucketing**:
  - Use the aligned value as a **bucket key** and group by it, usually followed
    by aggregation.
  - This is conceptually:

    ```clojure
    (let [aligned (down-to-nearest time-col 10 :seconds)]
      (-> ds
          (tc/add-column :bucket aligned)
          (tc/group-by :bucket)
          (tc/aggregate agg-spec)))
    ```

- **Resampling / adjust-frequency**:
  - A higher-level dataset operation that:
    - chooses a time coordinate (index or explicit column),
    - aligns it using `down-to-nearest`/`align-time`,
    - and then groups/aggregates and/or constructs a new regular time index.
  - This is where a future `adjust-frequency` or `resample-time` API will live,
    built on top of column primitives.
  - **Important**: This covers **downsampling/aggregation** (many → fewer points).

We will:

- Keep `down-to-nearest` (or a renamed `align-time`/`floor-time`) as the
  **primitive alignment** op (scalar + column), completely independent of any
  dataset index concept.
- Build dataset-level **resampling** (`adjust-frequency`, `resample-time`,
  etc.) in terms of:
  - a chosen time column (optionally marked via `index-by`),
  - column-level alignment (`down-to-nearest`), and
  - standard group/aggregate operations.

### 7.1. Interpolation / upsampling (distinct from bucketing/aggregation)

**Use case** (from [Zulip conversation with Daniel Slutsky](https://clojurians.zulipchat.com/#narrow/dm/138175,214379-dm/near/562511797)):

> Just to make it concrete with an example: the beats of the heart (or our estimates of when they happen) are irregular in time. Often, when we wish to conduct some analysis regarding heart rate or heart rate variability, we will interpolate and resample, so we have a regular time series, and then we can use methods like Fourier transform for frequency analysis.

This highlights a **second type of resampling** distinct from bucketing/aggregation:

#### Two resampling patterns:

1. **Downsampling/Aggregation** (many → fewer points)
   - Input: High-frequency irregular data (e.g., 1000 transactions/day, irregular heartbeats)
   - Output: Lower-frequency aggregated data (e.g., daily totals, minute-by-minute averages)
   - Operation: **Aggregate** multiple values into buckets (sum, mean, count, etc.)
   - Implementation: `rollup-every`, `add-bucket-column` (planned above)

2. **Upsampling/Interpolation** (fewer → more points, or irregular → regular)
   - Input: Irregular time series (e.g., heartbeats at 0ms, 850ms, 1720ms, 2540ms...)
   - Output: Regular time grid (e.g., every 100ms: 0ms, 100ms, 200ms, 300ms...)
   - Operation: **Interpolate** to estimate values at new time points
   - Use cases: Signal processing (FFT), regular time grids for ML, filling gaps

#### Proposed API:

```clojure
(resample-to-regular-grid [ds time-col value-col interval unit opts])
```

Example:
```clojure
(resample-to-regular-grid ds :beat-time :rr-interval 100 :milliseconds
  {:method :linear        ; or :ffill, :bfill, :nearest, :zero
   :start "2024-01-01"    ; optional: explicit start time
   :end "2024-01-02"})    ; optional: explicit end time
```

Common interpolation methods:
- `:ffill` - forward fill (last observation carried forward)
- `:bfill` - backward fill  
- `:linear` - linear interpolation between points
- `:nearest` - nearest neighbor
- `:zero` - fill missing with zeros

#### Implementation approach:

This would build on **column-level primitives** (to be added):

1. **Time grid generation**:
   ```clojure
   (generate-time-range [start end interval unit opts])
   ;; Returns a column of evenly-spaced time points
   ```

2. **Interpolation**:
   ```clojure
   (interpolate-values [time-col value-col new-times method opts])
   ;; For each time in new-times, interpolate value from surrounding points in time-col/value-col
   ```

3. **Dataset-level operation**:
   - Generate regular time grid (or accept explicit grid)
   - For each output time point, interpolate value from surrounding input points
   - Return new dataset with regular time index

**Implementation notes**:
- More complex than bucketing/aggregation because it requires:
  - Looking at neighboring points (not just within a bucket)
  - Mathematical interpolation logic (especially for `:linear`)
  - Handling edge cases (before first point, after last point)
- Could leverage dtype-next operations where applicable
- Consider whether to support multi-column interpolation (interpolate all numeric columns)

**Priority**: Medium-to-high, as it addresses a distinct and important use case (signal processing, ML pipelines) that cannot be satisfied by bucketing/aggregation alone.

Unit handling:

- We'll keep a small `normalize-unit` function at the tablecloth.time layer to
  normalize user-facing units (e.g. `:second` → `:seconds`, `:minute` →
  `:minutes`, `:week` → `:weeks`, etc.).
- Internally we will:
  - Map **duration-like** units (`:seconds`, `:minutes`, `:hours`, `:days`,
    `:weeks`, etc.) onto dtype-next's **relative dtypes** and constants.
  - Treat **calendar-like** units (`:months`, `:quarters`, `:years`) via
    `LocalDate`/`LocalDateTime` logic (month/quarter/year boundaries), not as
    simple fixed-length relative durations.

This aligns with how other ecosystems behave conceptually:

- pandas: `Timestamp.floor` / `Series.dt.floor` for alignment, `.resample()`
  for resampling.
- R (lubridate/dplyr): `floor_date()` for alignment,
  `group_by(floor_date(...)) %>% summarise(...)` for bucketing/resampling.
- SQL (Postgres): `date_trunc()` for alignment,
  `GROUP BY date_trunc(...)` for bucketing.

---

## 8. Rolling windows (in-process inquiry)

We have two different rolling window approaches to evaluate:

### 8.1. Archived implementation (`_archive/src/api/rolling_window.clj`)

**Type**: Fixed-size **row-count-based** rolling windows

**Key characteristics**:
- Window defined by **number of rows** (e.g., "previous 3 rows")
- Works on **any column** (time or not)—just uses positional indices
- Returns a **grouped dataset** where each group contains the window rows
- Implementation uses row indices and leverages `tc/group-by` with custom grouping map

**Use case example**:
```clojure
;; Window of previous 3 rows
(rolling-window ds 3)
;; → Each row gets a dataset containing [row-2, row-1, current-row]
```

**Characteristics**:
- Always same number of rows per window (except at start where truncated)
- Not time-aware—just counts rows
- General-purpose, could work on any ordered data

### 8.2. dtype-next's `variable-rolling-window-ranges`

**Type**: Time-based **duration windows**

**Key characteristics**:
- Window defined by **time duration** (e.g., "previous 5 minutes")
- Requires **sorted, monotonically increasing datetime column**
- Works with **irregular time series** (variable number of rows per window)
- Returns **index ranges** (just `[start-idx end-idx]` pairs, not datasets)
- Units in microseconds (or other time units via constants)

**Use case example**:
```clojure
;; Window of previous 5 minutes
(variable-rolling-window-ranges time-col 5 :minutes)
;; → For each time point, returns [start-idx end-idx] covering previous 5 minutes
;; → Variable number of rows per window depending on data density
```

**Characteristics**:
- Window size varies: sparse data = fewer rows, dense data = more rows
- Time-aware—uses actual temporal values
- Specifically designed for time-series analysis

### 8.3. Comparison

| Feature | Archived `rolling-window` | dtype-next `variable-rolling-window-ranges` |
|---------|--------------------------|---------------------------------------------|
| **Window definition** | Fixed row count | Time duration |
| **Result** | Grouped dataset | Index ranges |
| **Rows per window** | Always same (3 rows, 10 rows, etc.) | Variable (depends on time density) |
| **Time-aware?** | No (just counts rows) | Yes (uses actual time values) |
| **Use case** | "Last N observations" | "Last N minutes/hours/days" |
| **Data requirement** | Any ordered data | Sorted datetime column |

### 8.4. Design considerations (to revisit)

**For time-series work**, dtype-next's approach seems more appropriate because:
- Time-based windows are more meaningful: "5-minute rolling average" vs "3-row rolling average"
- Handles irregular data properly: If heartbeats are irregular, you want "last 10 seconds of beats" not "last 3 beats"
- More flexible: can build row-count windows on top of time windows, but not vice versa

**However, the archived version** has value as a general-purpose utility:
- Simpler for non-time data
- Returns grouped datasets (higher-level abstraction)
- Could potentially live in tablecloth itself (not time-specific)

**Open questions**:
1. Should we provide both? Row-count for simplicity, time-based for time-series?
2. What API should we expose? Grouped datasets vs index ranges vs something else?
3. How does this relate to aggregation? Do we want `(rolling-mean ds :value 5 :minutes)` or lower-level primitives?
4. Should row-count rolling windows live in `tablecloth.api` rather than `tablecloth.time`?

**Priority**: Medium—useful for time-series analysis but not as fundamental as slice/bucket/interpolate.

---

## 9. Reference: dtype-next datetime API

For details on the underlying dtype-next datetime namespace (`tech.v3.datatype.datetime`) that we are lifting into `tablecloth.time`, see:

- `doc/dtype-next-datetime-api-notes.md`
