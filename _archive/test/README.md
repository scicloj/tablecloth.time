# Archived Tests

This directory contains tests for legacy/defunct namespaces that have been removed from the active codebase but preserved for future reference.

## Purpose

These tests are kept as **reference material** for when we reimplement similar functionality in the new architecture. They provide valuable:
- Test cases and edge cases to consider
- Expected behavior and semantics
- Examples of how the old APIs were used

## Archived Test Files

- **adjust_frequency_test.clj** - Tests for the old `adjust-frequency` API (bucketing/resampling)
- **api_indexing_test.clj** - Tests for the old `index-by`/`rename-index` API
- **converters_test.clj** - Tests for the old converter functions (`->minutes`, `anytime->milliseconds`, etc.)
- **rolling_window_test.clj** - Tests for the old rolling window API
- **slice_test.clj** - Tests for the old slice/index-by API
- **time_components_test.clj** - Tests for the old time component extractors
- **utils_indexing_test.clj** - Tests for the old index utilities
- **validatable_test.clj** - Tests for the validatable utility (dataset integrity checking)

## Usage

These tests are **not run** as part of the test suite (they are outside the `test/` directory). To reference them:
1. Open the relevant test file to see test cases
2. Copy and adapt test cases when implementing new functionality
3. Use as a reference for expected behavior

## History

Archived on 2024-12-28 as part of the tablecloth.time refactoring to focus on the new column-based architecture described in `development-plan.md`. See also `_archive/src/` for the corresponding source code.
