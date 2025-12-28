# Archived Legacy Code

This directory contains legacy source code and tests that have been removed from the active codebase as part of the tablecloth.time refactoring (2024-12-28).

## Structure

- **`src/`** - Archived source namespaces (see `src/README.md` for details)
- **`test/`** - Archived test files (see `test/README.md` for details)

## Why Archive?

The tablecloth.time library is being refactored from an implicit index-based API to a new **column-based architecture** that:

- Uses **explicit column arguments** instead of metadata-based indexing
- Builds on `tablecloth.time.column.api` for column-level primitives
- Requires **sorted data** for time operations (no silent reordering)
- Uses **binary search** for efficient time slicing
- Follows tablecloth's existing patterns (`group-by`, `order-by`, etc.)

The old APIs (`index-by`, `adjust-frequency`, `rolling-window`, etc.) are being reimplemented in this new architecture. Note: `slice.clj` has been restored to the active codebase as work in progress. Rather than delete them entirely, we've archived them as **reference material** for:

- Test cases and edge cases
- Expected behavior and semantics
- Implementation patterns
- API design examples

## Not Loaded

Files in this directory are **not loaded** by the application:
- They are outside the `src/` and `test/` directories
- Leiningen does not compile or test them
- They exist purely for reference

## See Also

- `development-plan.md` - Full details on the new architecture
- Active column API: `src/tablecloth/time/column/api.clj`
- Active tests: `test/tablecloth/time/column/api_test.clj`
