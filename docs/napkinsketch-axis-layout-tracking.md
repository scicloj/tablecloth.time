# Napkinsketch axis layout tracking

_Last updated: 2026-04-05_

## Context

This note tracks the April 2026 experiment debugging x-axis label layout in the `chapter_02_napkinsketch.clj` notebook for `tablecloth.time`.

Primary local files involved:
- `notebooks/chapter_02_napkinsketch.clj`
- `../napkinsketch/src/scicloj/napkinsketch/impl/sketch.clj`
- `../napkinsketch/src/scicloj/napkinsketch/render/panel.clj`
- `../napkinsketch/src/scicloj/napkinsketch/render/membrane.clj`

## What we learned

### 1. The `:Month` column was not being treated as categorical

After inspecting the local `napkinsketch` code, the important finding was:
- `column-type` recognizes dtype-next datetime columns as `:temporal`
- temporal x-columns are converted to epoch milliseconds internally
- datetime ticks/labels are generated via a Wadogo `:datetime` scale

So the core problem was **not** temporal inference.

### 2. The first visible issue was x-tick label layout

The original problem was cramped time labels on the x-axis. We tested and confirmed:
- the plan looked sensible
- tick labels were short and reasonable
- the weak point was label rendering/layout, not scale inference

### 3. First improvement: rotate dense x tick labels

We added an initial x-axis tick heuristic in local `napkinsketch` work:
- estimate label width from label length and font size
- detect when labels are too dense for the panel width
- rotate x tick labels when needed

This improved the chart, but exposed a second issue.

### 4. The follow-up issue exposed a layout abstraction problem

After rotating tick labels, the axis title (`Month`) overlapped the rotated ticks.

This exposed a structural problem in the preexisting layout abstraction:
- `compute-layout-dims` computes quantities like `x-label-pad` and `total-height`
- `plan->membrane` later re-derives axis-title placement from those quantities
- one of the formulas effectively used `x-label-pad` both to reserve space and to position the label

Concrete example:
- `x-label-pad` contributes to `total-height`
- then x-label rendering used a formula like `(- total-height x-label-pad -2)`
- so changing `x-label-pad` could be canceled out by later rendering math

This is the key conceptual lesson from the experiment.

## Current design insight

The cleaner direction is to stop treating `x-label-pad` as one overloaded number and instead represent the underlying ideas explicitly.

What seems needed conceptually:
- x tick-label footprint
- gap between x ticks and axis title
- axis-title space/offset
- total bottom reserve derived from those parts

Short version:
- **reserved space** and **render placement** should not be coupled indirectly through the same vague padding quantity
- both should derive from a more explicit axis-layout model

## Interim local experiments

We tried a few incremental changes locally:
- extracting x-axis tick heuristics into a shared helper
- estimating rotated tick-label footprint using a geometric approximation
- experimenting with edge-relative axis-title placement

These were useful diagnostically, but the broader lesson was more important than any one patch: the abstraction boundary between planning and rendering is the real thing to fix.

## Geometric heuristic captured during the experiment

For a rotated text box approximation, vertical footprint was estimated as:

`sin(theta) * label-width + cos(theta) * label-height`

with:
- `theta` in radians
- `label-width` estimated heuristically from average label length and font size
- `label-height` approximated by font size

This was useful for reasoning, even if the final abstraction may change.

## Practical next step

A reasonable next refactor in `napkinsketch` would be:
- introduce a clearer axis-layout representation
- make axis-title placement edge-relative
- ensure figure sizing and axis-title placement are derived from the same explicit axis-layout quantities

That is likely a better long-term fix than stacking more one-off adjustments onto x-label rendering.
