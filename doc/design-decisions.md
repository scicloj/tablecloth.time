# Design Decisions

## "A Vectorized Relational Database" (Conceptual Framing)

From a Zulip exchange (Oct 2021), Awb99 jokingly described tech.ml.dataset as "a vectorized relational database" — but it captures something essential:

- **Vectorized** — columnar, efficient operations over columns
- **Relational** — filter, join, group, aggregate
- **Not a database** — no persistence, no query optimizer, no indexes

It's almost an anti-database: all the relational algebra, none of the infrastructure for durability and incremental updates. This is exactly why tree-based indexes don't make sense for TMD — you're not maintaining state across transactions, you're rebuilding from scratch each time.

This framing helps explain the design choice: in a traditional database, indexes exist to speed up queries on data that persists and changes incrementally. In TMD, datasets are rebuilt wholesale, so the cost of constructing a tree negates its benefit. Binary search on sorted data is simpler and equally fast.

---

## Metadata vs Composable Helpers (Feb 2026)

### Context
While implementing fpp3 Chapter 2 seasonal plots, we needed to extract many time fields and compute derived columns (phases, string conversions) from datetime columns. This raised the question: should tablecloth.time adopt tsibble-like metadata, or stick with explicit composable helpers?

### The Two Approaches

#### Option 1: tsibble-like Metadata
In R, `tsibble` attaches metadata to data frames:
```r
vic_elec <- as_tsibble(data, index = Time, key = c(State, Region))
```

From this declaration, tsibble automatically:
- Detects interval (half-hourly, daily, etc.) via GCD of time differences
- Enables smart plotting functions like `gg_season(period = "day")`
- Hides complexity behind the metadata

**Pros:**
- Less typing (declare once, infer later)
- "Just works" feel, closer to R ergonomics
- Attracts users who want pandas/R-like convenience

**Cons:**
- More magic (where did this come from?)
- Harder to debug
- Requires metadata infrastructure
- New pattern for tablecloth ecosystem

#### Option 2: Composable Helpers (chosen)
Extend `add-time-columns` with more field types. User explicitly requests what they need:
```clojure
(time-api/add-time-columns ds "Time"
  {:daily-phase "DailyPhase"
   :year-string "YearStr"
   ...})
```

**Pros:**
- Transparent — you see exactly what's computed
- Easy to inspect/debug
- Fits existing tablecloth philosophy
- No new infrastructure needed
- Composition over magic

**Cons:**
- More typing
- User must know what fields exist
- Not as "batteries included" as R

### Decision
We chose **composable helpers** because:

1. **Fits tablecloth's philosophy**: "composition over magic" is a core principle
2. **We're early**: Still learning patterns from fpp3; premature to commit to metadata system
3. **Community split**: SciCloj has ongoing discussion about this tradeoff (some prefer transparency, others want R-like ergonomics)
4. **Incremental path**: Can always add metadata layer later once patterns stabilize

### Implementation
Extended `add-time-columns` with computed fields:
- `:hour-fractional`, `:daily-phase`, `:weekly-phase` — for seasonal x-axes
- `:date-string`, `:year-string`, `:month-string`, etc. — for grouping/coloring
- `:week-index`, `:year-week-string` — for weekly plots (avoids ISO week issues)

### Future Considerations
- Could add optional interval detection as a utility function
- Could build higher-level `seasonal-plot` that infers what it needs
- Metadata approach remains possible if patterns stabilize and community wants it

### Related Discussions
- SciCloj Zulip: "caching with Pocket" thread (Feb 2026) — parallel discussion about computation DAGs vs data metadata
- tableplot gaps doc: `doc/tableplot-gaps.md`

## Future Consideration: Time-Aware Lag

Currently `add-lag` and `add-lead` are purely row-based — they shift by N positions regardless of timestamps. This requires users to:
1. Know their data's frequency
2. Calculate row offsets manually (e.g., 48 rows = 24h at half-hourly)
3. Assume no gaps in the data

**Pandas comparison:**
```python
df['col'].shift(1)           # row-based (like ours)
df['col'].shift(freq='1D')   # time-aware — aligns by timestamp
```

**Potential API:**
```clojure
;; Current (row-based)
(add-lag ds :Demand 48 :Demand_yesterday)

;; Possible time-aware version
(add-lag ds :Demand {:hours 24} :Demand_yesterday {:time-col :Time})
```

Time-aware lag would:
- Use the time column to compute correct offset
- Handle irregular data and gaps correctly
- Be more intuitive for users ("24 hours ago" vs "48 rows")

This could be added as an optional mode while keeping row-based as default for performance/simplicity.

---

## OPEN: Timezone Handling — Epoch-as-Truth vs Local-Values-as-Truth (Mar 2026)

**Status: Needs resolution soon — blocking correct seasonal plot implementation.**

### The Problem
Working through fpp3 Chapter 2 with vic_elec data, we discovered our seasonal plots were wrong. The CSV contains UTC timestamps (`2011-12-31 13:00:00`) but represents Melbourne local events. Our `add-time-columns` extracts hour=13 (UTC) when it should extract hour=0 (Melbourne midnight).

### Current Architecture: Epoch-as-Truth
`convert-time` uses epoch milliseconds as the universal pivot:
```
source type → epoch millis (using zone for interpretation) → target type
```
The `:zone` option controls how to interpret/render during conversion, but epoch millis is the underlying "truth."

### Alternative: Local-Values-as-Truth (Polars model)
Polars has two distinct operations:
- `replace_time_zone(zone)` — stamp zone onto naive datetime, local values unchanged
- `convert_time_zone(zone)` — convert zoned datetime to another zone, same instant

This treats local values as meaningful in themselves, with zone as attached metadata.

### The Tension
If you have `LocalDateTime "13:00"`:

**Path A (replace-time-zone):**
```clojure
(replace-time-zone col "Australia/Melbourne")
;; → ZonedDateTime 13:00+11:00 Melbourne
;; "13:00 IS Melbourne local time"
```

**Path B (epoch pivot):**
```clojure
(convert-time col :zoned-date-time {:zone "Australia/Melbourne"})
;; → Depends on assumed source zone
;; "13:00 in ???-zone → epoch → Melbourne"
```

Two different mental models. Both valid. Which should tablecloth.time embrace?

### Java.time Parallel
Java.time supports both:
- `localDateTime.atZone(zone)` — stamp zone (local-values-as-truth)
- `zonedDateTime.withZoneSameInstant(zone)` — convert zone (epoch-as-truth for the conversion)

The types themselves carry the semantics: `LocalDateTime` = naive, `ZonedDateTime` = zone-aware.

### Questions to Resolve
1. Should we add `replace-time-zone` and `convert-time-zone` as first-class operations?
2. If so, how do they interact with `convert-time`?
3. Does this create confusing multiple paths to the same result?
4. Is epoch-as-pivot still the right model, with these as convenience wrappers?

### Immediate Workaround
For vic_elec, can use Java interop:
```clojure
(tc/update-columns ds "Time"
  (fn [col]
    (tcc/column
      (map #(-> % (.atZone (ZoneId/of "UTC"))
                  (.withZoneSameInstant (ZoneId/of "Australia/Melbourne")))
           col))))
```

### Related
- Polars docs: `dt.replace_time_zone`, `dt.convert_time_zone`
- This also connects to the index question — tsibble's index carries timezone implicitly
