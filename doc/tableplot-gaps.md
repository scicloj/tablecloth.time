# Tableplot Gaps

Issues and missing features encountered while translating fpp3 examples.

## Discovered via fpp3 Chapter 2

### Numeric columns treated as continuous color scales
- **Context:** §2.4/2.5 seasonal plots — `:=color "MonthNum"` where MonthNum is `long`
- **Problem:** Tableplot treats numeric columns as continuous gradients, not discrete categories
- **Workaround:** Convert to string before plotting (`mapv str`)
- **Desired:** Option to force categorical treatment, e.g. `:=color-type :nominal`

### No seasonal plot helper
- **Context:** §2.4 `gg_season()` equivalent
- **Problem:** Building a seasonal overlay plot requires manual field extraction + color grouping
- **Note:** Partially addressed by `add-time-columns` in tablecloth.time. A `layer-seasonal` in tableplot would complete it.

### No subseries plot / faceting by field
- **Context:** §2.5 `gg_subseries()` equivalent
- **Problem:** No built-in faceting support (one panel per month, with mean line per panel)
- **Workaround:** Used color grouping as a poor substitute

### No scatterplot matrix
- **Context:** §2.6 `GGally::ggpairs()` equivalent
- **Problem:** No built-in pairwise scatterplot grid
- **Workaround:** Manual composition of individual scatterplots

### No lag plot helper
- **Context:** §2.7 `gg_lag()` equivalent
- **Problem:** Plotting y_t vs y_{t-k} requires manual column construction
- **Note:** Could be a tablecloth.time function (`lag-column`) rather than a tableplot feature

### No ACF bar plot with significance bounds
- **Context:** §2.8 ACF correlogram
- **Problem:** No built-in way to overlay horizontal reference lines (±1.96/√T bounds) on a bar chart
- **Workaround:** Added bound columns to dataset, but can't render them as horizontal lines

---

## Template for future entries

### [Short title]
- **Context:** fpp3 §X.Y, [R function]
- **Problem:** [What's missing or broken]
- **Workaround:** [What we did instead]
- **Desired:** [What the ideal API would look like]
