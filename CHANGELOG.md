# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [1.00-alpha-4] (2021-10-16)
### Changed
- Changed `adjust-interval` to `adjust-frequency`. This function now
  returns an ungrouped dataset by default. This behavior can be
  overriden by specifying the `ungrouped?` option
  ([PR](https://github.com/scicloj/tablecloth.time/commit/dccd91db86d4bf83d41311120a0b7eb9c1c20008)).
- Internal changes that make working with the column indexes more easy:
  - Added a `get-index-column-or-error` function that is used
    internally by functions that need to fetch the index. This
    function replaced a series of other helper functions, i.e. this
    simplifies the internal API for working with the index.
  - Used the `validatable` abstraction added by @daslu to set the
    index column on a dataset when we call `index-by`. Validatable
    makes it possible to check if the index has become invalid because
    the column data has changed in some way that invalidates the
    column.

## [1.00-alpha-3] (2021-06-01)
### Added
- New `string->time` method will try to parse a string to the correct
  time object. Currently, it expects the same default string formats
  that each java.time object's `parse` method expects.
- `convert-to` method that can convert one time to another.

## Changed
- [time-literals](https://github.com/henryw374/time-literals) added as a
  dependency.

### Removed
- [tick](https://github.com/juxt/tick) removed as a dependency

## 1.00-alpha-2 (2021-05-24)
### Changed
- `slice` now uses [tech.ml.dataset](https://github.com/techascent/tech.ml.dataset)'s new index-structure ([PR](https://github.com/techascent/tech.ml.dataset/pull/214))
- Documentation on how to make the widgets.

### Removed
- `tablecloth.time.index` namespace - not needed anymore

## 1.00-alpha1
- First alpha release

[Unreleased]: https://github.com/scicloj/tablecloth.time/compare/1.00-alpha-3...HEAD
[1.00-alpha-3]: https://github.com/scicloj/tablecloth.time/compare/1.00-alpha-2...1.00-alpha3
