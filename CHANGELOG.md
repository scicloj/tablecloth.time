# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

## [1.00-alpha-3] (2021-06-01)
### Added
- New `string->time` method will try to parse a string to the correct
  time object. Currently, it expects the same default string formats
  that each java.time object's `parse` method expects.
- `convert-to` method that can convert one time to another.

## Changed
- [time-literals](https://github.com/henryw374/time-literals) as a
  dependency.

### Removed
- [tick](https://github.com/juxt/tick) as a dependency

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
