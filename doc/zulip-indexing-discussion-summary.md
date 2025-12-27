# Zulip Discussion Summary: Indexing for Time Series

_Source: Clojurians Zulip #tech.ml.dataset.dev > "index structures in Columns - scope" thread_  
_Date: Late 2024 (December)_

## Context

Discussion about index structures for tablecloth.time, exploring whether we need tree-based structures (like pandas DateTimeIndex) or simpler approaches for time-based slicing and lookups. The conversation was prompted by two needs:

1. **Categorical index** (lookup table) - for Pandas-style index behavior in tutorials
2. **Ordinal/time index** - for tablecloth.time revival after index removal from tech.ml.dataset

## Key Participants

- **Daniel Slutsky**: Coordinating tablecloth.time revival and seeking indexing approach
- **Ethan Miller**: Original tablecloth.time author, seeking to revive after index removal from tech.ml.dataset
- **Harold**: Exploring categorical and temporal indexing patterns, provided real-world performance data
- **Chris Nuernberger**: dtype-next author, provided critical performance guidance
- **jsa**: Suggested interval trees
- **Will Cohen**: Working on geospatial index structures

## Main Insights

### 1. Binary Search > Tree Structures for Datasets (Chris Nuernberger)

**Quote:**
> "You only need tree structures if you are adding values ad-hoc or removing them - usually with datasets we aren't adding/removing rows but rebuilding the index all at once. **Just sorting the dataset and using binary search will outperform most/all tree structures in this scenario as it is faster to sort than to construct trees. Binary search performs as well as any tree search for queries and range queries.**"

**Rationale:**
- Datasets are typically **rebuilt/reloaded** wholesale, not incrementally modified
- **Sorting is faster than constructing trees** when building from scratch
- **Binary search performs as well as tree search** for queries and range queries
- Simpler implementation with predictable performance

**Performance validation (Harold):**
> "I'm actually getting surprisingly good performance out of `java.util.Collections/binarySearch` + `tech.v3.dataset/row-map`, I can decorate the example dataset above in less than 1s (and the real-world dataset that has more than 1M rows also at a rate greater than 1M rows/s)."

### 2. Categorical Indexing Pattern (Harold)

For categorical/hash-based lookups (e.g., lookup by ID):

```clojure
(defn obtain-index [ds colname]
  (let [m (ds/group-by-column->indexes ds colname)]
    (fn [v]
      (map (partial ds/row-at ds) (get m v)))))

(def lookup (obtain-index ds :id))
(lookup "a")  ; returns rows where :id == "a"
```

**Properties:**
- **O(n) to build** (optimal - must scan all rows once)
- **O(1) lookup** via hash map
- Returns **closure/function** capturing dataset value
- Rows realized **lazily**
- No data copying - serves directly from dataset
- `group-by-column->indexes` already exists in tech.ml.dataset

### 3. Time-Based Indexing Pattern

For ordered/temporal lookups (e.g., time slicing):

**Approach:**
1. **Sort dataset** by time column (if not already sorted)
2. Use **binary search** to find range boundaries
3. Return `(tc/select-rows ds (range start-idx end-idx))`

**Key operations:**
- `java.util.Collections/binarySearch` or custom binary search for lower/upper bounds
- dtype-next provides efficient sorting and comparison for datetime columns
- Can check sortedness in O(n) and error if not sorted (or use `:sorted? true` optimization hint)

### 4. tablecloth.time Design Implications

Based on Chris's guidance and Harold's validation:

**What we need:**
- Sort + binary search (not tree structures)
- Explicit column arguments (consistent with tablecloth's API)
- Sortedness checking with helpful error messages
- Millis-pivot approach for time arithmetic (already in tablecloth.time)

**What we DON'T need:**
- Tree-based index structures (TreeMap, interval trees, etc.)
- Metadata-based axis marking (`index-by`)
- Complex index caching/lifecycle management

### 5. Real-World Problem Example (Harold)

**Problem:** 1M row time series dataset; for each row, find the index of the row closest to one week later.

**Solution:** Binary search per row: >1M rows/s performance using `java.util.Collections/binarySearch`

**Takeaway:** Binary search is fast enough even when doing 1M searches on 1M rows.

## Additional Topics Discussed

### 6. Pandas Index Behavior and Inconsistencies (Harold)

Harold discovered that pandas indexing has **type inconsistencies** - the type returned by a lookup varies based on the number of matching elements:

```python
s = pd.Series(range(3), index=list("aab"))
type(s.loc["a"])  # <class 'pandas.core.series.Series'> (2 matches)
type(s.loc["b"])  # <class 'numpy.int64'> (1 match)
```

Similarly for DataFrames:
```python
type(df.loc["a"])  # <class 'pandas.core.frame.DataFrame'> (multiple matches)
type(df.loc["b"])  # <class 'pandas.core.series.Series'> (single match)
```

**Our decision:** Avoid this inconsistency. Our indexing functions should return consistent types.

### 7. Why Index Was Removed from tech.ml.dataset (Harold's perspective)

Harold noted:
> "I think indexing in particular is a bit of a sprawling topic, and there isn't yet a great consensus on what to do for it from a functional data science perspective."

> "I have been reading Pandas' source in this area, and it's a bit bonkers. I would estimate there is about as much code in Pandas doing indexing stuff as there is _all of the code in TMD_"

The removal was part of **simplification** - keeping tech.ml.dataset focused on core functionality, with extensions built outside.

### 8. Multiple Values Search Algorithm (Daniel Slutsky)

Daniel suggested an advanced technique for searching multiple sorted values in a sorted dataset simultaneously, where searchers "learn from each other":

**Papers mentioned:**
- [Multiple Values Search Algorithm](https://www.researchgate.net/publication/254560725_Multiple_Values_Search_Algorithm)
- [GitHub implementation](https://github.com/juliusmilan/multi_value_binary_search)

**Concept:** When searching for many sorted values (e.g., t+week for all t), maintain multiple binary search ranges that collapse when pointers collide. Could be beneficial when values are dense.

**Status:** Interesting optimization for future, but standard binary search is sufficient for now.

### 9. Interval Trees and BRIN Indexes

**jsa** mentioned interval trees as potentially useful for grouping/considering intervals as "the same."

**Harold** mentioned Postgres **BRIN indexes** (Block Range Index) as useful for time series:
> "For time series we've had great experience w/ Postgres' BRIN indexes"

**Status:** Not needed for initial implementation given binary search performance.

### 10. tablecloth.time's Core Focus (Ethan Miller)

Ethan clarified what tablecloth.time was about:
- **Slice function** using index structure for time series segmentation
- **Rolling average** function
- **Time manipulation abstractions** using tech.ml.dataset/dtype-next's **millis-pivot approach**
  - Clear time conversion pathway through conversion to milliseconds
  - Advantages over libraries based directly on Java's time classes

The indexing mechanism was secondary; the real value was making time operations **concise and idiomatic**.

### 11. Development Philosophy (Harold)

Harold outlined a pragmatic approach to feature development:

> "Our policy is always to implement the first version of something inside a specific leaf (client) project that needs it. Then, if that thing is later needed elsewhere, it can be lifted into a library that those projects share."

> "What I'd really like to see is examples of powerful uses of indexing in Pandas that lead to enviable solutions to actual problems. Then we can look at those and see what features they imply we need."

**Implication for tablecloth.time:** Start simple (explicit column args, binary search), iterate based on real use cases.

### 12. Geospatial Context (Will Cohen)

Will Cohen was working on geospatial index structures in parallel. Discussion touched on:
- Custom datatypes in tech.ml.dataset (`:geometry` vs `:object`)
- Protocol vs interface/baseclass for `add-object-datatype!`
- Performance implications of `satisfies?` (Chris: "unnecessarily slow")

**Relevance:** Demonstrates dtype-next's extensibility for custom spatial types, but not directly applicable to time indexing.

## Comparison to Pandas

**Pandas approach:**
- Complex DateTimeIndex infrastructure
- ~As much indexing code as all of tech.ml.dataset (Harold's observation)
- Type inconsistencies (single vs multiple results return different types)
- Heavy abstraction, complex to understand

**Our approach (tablecloth.time):**
- Explicit column arguments: `(slice-by ds :time-col start end)`
- Simple implementation: sort + binary search
- Consistent types: always returns dataset/rows
- Fast: >1M rows/s demonstrated
- Millis-pivot for time arithmetic

## Inspirational Resources Mentioned

- **Python Data Science Handbook** - Pandas index examples
- **Jake VanderPlas**: [Working with Time Series](https://jakevdp.github.io/PythonDataScienceHandbook/03.11-working-with-time-series.html)
- **R lubridate**: [Dates and times](https://r4ds.had.co.nz/dates-and-times.html)
- **Postgres BRIN indexes**: [Block Range Index](https://en.wikipedia.org/wiki/Block_Range_Index)

## References

- Original Zulip thread: [index structures in Columns - scope](https://clojurians.zulipchat.com/#narrow/channel/236259-tech.2Eml.2Edataset.2Edev/topic/index.20structures.20in.20Columns.20-.20scope)
- dtype-next argops: `tech.v3.datatype.argops/arggroup`
- tech.ml.dataset: `ds/group-by-column->indexes`, `ds/row-at`
- Java binary search: `java.util.Collections/binarySearch` and `java.util.Arrays/binarySearch`
- Multiple value search papers (Daniel's suggestion):
  - [Multiple Values Search Algorithm](https://www.researchgate.net/publication/254560725_Multiple_Values_Search_Algorithm)
  - [GitHub implementation](https://github.com/juliusmilan/multi_value_binary_search)

## Action Items for tablecloth.time

Based on the discussion, here's what we should implement:

### Immediate (MVP)
1. ✅ **Use binary search** (always, unconditionally) per Chris N's guidance
2. ✅ **Explicit column arguments** (no metadata-based `index-by`) per tablecloth consistency
3. ✅ **Check sortedness** with helpful errors; `{:sorted? true}` optimization hint
4. ✅ **Leverage Java binary search** (`java.util.Collections/binarySearch`) or write custom lower/upper bound search
5. ✅ **Focus on millis-pivot** conversions and time arithmetic (the real value-add per Ethan)

### Deferred (unless compelling use case emerges)
- Tree-based indexes (TreeMap, interval trees, BRIN-style)
- Metadata-based index tracking
- Multiple-value search optimizations
- Categorical index closure helpers (can use `ds/group-by-column->indexes` directly)

### Out of Scope
- Geospatial indexes (handled by separate library)
- Complex pandas-style DateTimeIndex API
- Type-inconsistent return values
