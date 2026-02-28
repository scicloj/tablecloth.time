# Design Decisions

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
