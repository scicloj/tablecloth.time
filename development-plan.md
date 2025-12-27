# tablecloth.time – development plan

_Last updated: (fill via git history)_

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

2. Provide an explicit way to **mark** the time axis at the dataset level, e.g.:

   ```clojure
   (tct/index-by ds :received-date)
   ```

   which can:
   - Record metadata: `{:time-axis {:column :received-date :sorted? true? ...}}`.
   - Optionally ensure (or record) sortedness.

3. Time-based operations (slicing, rolling) can:
   - Take a **time column argument** explicitly (most honest and clear), or
   - Default to the column recorded in `:time-axis` metadata when present.

4. We **do not** cache closures tied to specific dataset values in metadata, because datasets are immutable and transforms produce new values. We only cache **declarative information** (which column is the axis, sortedness, units, etc.).

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
- Use **binary search** or dtype-next's time-window operations (e.g. `variable-rolling-window-ranges`) over that column.
- Optionally, we could provide a `obtain-time-index` helper that returns a closure capturing a sorted time column + row indices, but we should not blindly cache that closure in dataset metadata.

In practice for tablecloth:

- Index awareness means: knowing **which column is time** and, when required, assuming/enforcing sortedness.
- We base slicing, rolling, etc. on this column; we do not need a separate Index type.

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

On top of column primitives and the time-axis concept, we can define dataset-level helpers that feel natural in tablecloth:

### 5.1. Marking the time axis

```clojure
(index-by [ds time-col])
```

- Records, in metadata, that `time-col` is the time axis.
- Optionally ensures sortedness and stores `:sorted? true`.

### 5.2. Slicing by time

Two styles:

- **Explicit time column:**

  ```clojure
  (slice [ds time-col start end])
  ```

- **Implicit via index-by metadata:**

  ```clojure
  (-> ds
      (index-by :received-date)
      (slice "2022-01-01" :end-of-year))
  ```

Internal approach:

- Parse `start`/`end` using millis-pivot semantics (string/date/instant → millis).
- Normalize the chosen time column to millis and filter rows between bounds.
- Implementation may use binary search if the axis is sorted and large; otherwise simple predicate filtering.

### 5.3. Bucketing and rollups

Examples of higher-level helpers built on `bucket-every-col`:

- Add a bucket column:

  ```clojure
  (add-bucket-column [ds time-col new-col interval unit opts])
  ```

- Roll up by intervals:

  ```clojure
  (rollup-every [ds time-col interval unit agg-spec opts])
  ```

Implementation pattern:

- Compute bucket column via `bucket-every-col`.
- Add it via `tc/add-column` / `tc/add-columns`.
- `tc/group-by` the bucket column.
- `tc/aggregate` according to `agg-spec`.

Here, **index-awareness is limited** to knowing which column to treat as time. The actual grouping and aggregation uses standard dataset operations.

---

## 6. Practical next steps

1. **Column API:**
   - Define `tablecloth.column.time` with:
     - `->millis-col`
     - `millis->datetime-col`
     - `bucket-every-col`
     - (optionally) a few basic field extractors over datetime columns.

2. **Dataset API:**
   - Add `index-by` / `with-time-axis` to mark a time axis.
   - Implement `slice` that:
     - Can take an explicit time column.
     - Or uses the axis from metadata.
   - Implement `add-bucket-column` and `rollup-every` built on `bucket-every-col` and standard `group-by`/`aggregate`.

3. **Reuse from gnomon:**
   - Port tests and semantics from the gnomon repo (especially for `down-to-nearest`, `->every`, and conversion behaviors) into `tablecloth.time` tests.
   - Use those semantics to guide the column-level and dataset-level implementations.

4. **De-prioritize gnomon as a standalone library** until/unless a clear general JVM use case emerges.

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

We will:

- Keep `down-to-nearest` (or a renamed `align-time`/`floor-time`) as the
  **primitive alignment** op (scalar + column), completely independent of any
  dataset index concept.
- Build dataset-level **resampling** (`adjust-frequency`, `resample-time`,
  etc.) in terms of:
  - a chosen time column (optionally marked via `index-by`),
  - column-level alignment (`down-to-nearest`), and
  - standard group/aggregate operations.

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

## 8. Reference: dtype-next datetime API

For details on the underlying dtype-next datetime namespace (`tech.v3.datatype.datetime`) that we are lifting into `tablecloth.time`, see:

- `doc/dtype-next-datetime-api-notes.md`
