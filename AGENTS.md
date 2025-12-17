# Agent guide for tablecloth.time

Always read `development-plan.md` before doing any work in this repo, even if you're only consulting or answering questions; it contains essential context for the current roadmap and design decisions.

## Build / lint / test
- Lint (cljfmt + clj-kondo): run `lein lint` from the repo root.
- Run all tests (preferred): `lein midje` (runs Midje + clojure.test).
- Run all clojure.test tests only: `lein test`.
- Run a single test namespace: `lein test tablecloth.time.api.adjust-frequency-test`.
- Run a single test var: `lein test :only tablecloth.time.api.adjust-frequency-test/test-adjust-frequency`.
- Optional REPL/dev: `clj -M:dev` or `lein repl`.

## Code style
- Use 2-space indentation; keep lines reasonably short (<100 chars).
- Public API fns (in `tablecloth.time.api*`) should have docstrings and clearly documented options.
- Namespace layout: `tablecloth.time.<area>[.<component>]`, tests in matching `..._test.clj` namespaces.
- Requires: group external libs then internal `tablecloth.time.*`; prefer `:as` for big APIs and `:refer` for a few symbols.
- Prefer pure, small functions; use data-first style and threading/conditional macros like `cond->`.
- Options maps: use keyword keys, destructure with `:keys [] :or {}`, and name booleans with a `?` suffix.
- Error handling: return nil for “not found” cases; otherwise throw an `Exception.` or `ex-info` with a clear, user-facing message (see `unidentifiable-index-error`).
- Predicates and queries end in `?`; helper constants are lower-case with hyphens.
- Use `clojure.test` (`deftest`, `testing`, `is`) for tests; mirror the style in existing test namespaces.
- Run `lein lint` before committing to keep cljfmt/clj-kondo happy.
