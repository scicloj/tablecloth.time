# Archived Source Code

This directory contains legacy/defunct source namespaces that have been removed from the active codebase but preserved for future reference.

## Purpose

These source files are kept as **reference material** for when we reimplement similar functionality in the new column-based architecture described in `development-plan.md`. They provide:
- Implementation patterns and approaches
- Edge case handling
- API design examples

## Archived Source Files

### API Namespaces (`api/`)

- **adjust_frequency.clj** - Old frequency adjustment/bucketing API (to be reimplemented)
- **indexing.clj** - Old index-by/rename-index API (to be reimplemented with explicit column args)
- **rolling_window.clj** - Old rolling window API (to be reimplemented)
- ~~**slice.clj**~~ - RESTORED to active codebase (work in progress)

### Utility Namespaces (`utils/`)

- **datatypes.clj** - Datatype utility functions (used by archived APIs)
- **indexing.clj** - Index management utilities (used by archived APIs)
- **validatable.clj** - Dataset integrity checking utility

## Usage

These files are **not loaded** by the application (they are outside the `src/` directory). To reference them:
1. Open the relevant source file to see implementation details
2. Copy and adapt code when implementing new functionality
3. Use as a reference for handling edge cases and API design

## Corresponding Tests

See `_archive/test/` for the test files that covered these namespaces.

## History

Archived on 2024-12-28 as part of the tablecloth.time refactoring to focus on the new column-based architecture. The new architecture:
- Uses explicit column arguments instead of implicit index metadata
- Builds on `tablecloth.time.column.api` for column-level primitives
- Requires sorted data for time operations (no silent reordering)
- Uses binary search for efficient time slicing

See `development-plan.md` for full details on the new architecture.
