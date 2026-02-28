# fpp3 Workthrough — Forecasting: Principles and Practice (3rd ed)

Book: https://otexts.com/fpp3/
Goal: Work through chapter by chapter, learning concepts and building tablecloth.time as we go.

## R → Clojure Translation Guide

| R (fpp3 stack)         | Clojure equivalent                          |
|------------------------|---------------------------------------------|
| `tsibble`              | `tech.ml.dataset` + tablecloth              |
| `tibble` / `dplyr`     | `tablecloth.api`                            |
| `lubridate`            | `tablecloth.time.column.api` + java.time    |
| `ggplot2`              | TBD (Hanami? Vega? Clerk?)                  |
| `feasts` (features)    | tablecloth.time (to build)                  |
| `fable` (models)       | tablecloth.time or separate lib (to build)  |
| `fpp3` (datasets)      | Need to port or load equivalent datasets    |

## Progress

### Chapter 1: Getting started
- [ ] Read through
- [ ] Notes on key concepts

### Chapter 2: Time series graphics
- [ ] 2.1 tsibble objects
- [ ] 2.2 Time plots
- [ ] 2.3 Time series patterns
- [ ] 2.4 Seasonal plots
- [ ] 2.5 Seasonal subseries plots
- [ ] 2.6 Scatterplots
- [ ] 2.7 Lag plots
- [ ] 2.8 Autocorrelation
- [ ] 2.9 White noise

### Chapter 3: Time series decomposition
- [ ] 3.1 Transformations and adjustments
- [ ] 3.2 Time series components
- [ ] 3.3 Moving averages
- [ ] 3.4 Classical decomposition
- [ ] 3.5 Methods used by official statistics agencies
- [ ] 3.6 STL decomposition

### Chapter 4: Time series features
- [ ] 4.1 Some simple statistics
- [ ] 4.2 ACF features
- [ ] 4.3 STL features
- [ ] 4.4 Other features
- [ ] 4.5 Exploring Australian tourism data

### Chapter 5: The forecaster's toolbox
- [ ] 5.1 A tidy forecasting workflow
- [ ] 5.2 Some simple forecasting methods
- [ ] 5.3 Fitted values and residuals
- [ ] 5.4 Residual diagnostics
- [ ] 5.5 Distributional forecasts and prediction intervals
- [ ] 5.6 Forecasting using transformations
- [ ] 5.7 Forecasting with decomposition
- [ ] 5.8 Evaluating point forecast accuracy
- [ ] 5.9 Evaluating distributional forecast accuracy
- [ ] 5.10 Time series cross-validation

### Chapter 6: Judgmental forecasts
- [ ] Read through (conceptual, not much to implement)

### Chapter 7: Time series regression models
- [ ] 7.1–7.9

### Chapter 8: Exponential smoothing
- [ ] 8.1–8.7

### Chapter 9: ARIMA models
- [ ] 9.1–9.9

### Chapter 10: Dynamic regression models
- [ ] 10.1–10.6

### Chapter 11: Hierarchical and grouped time series
- [ ] 11.1–11.6

### Chapter 12: Advanced forecasting methods
- [ ] 12.1–12.5
