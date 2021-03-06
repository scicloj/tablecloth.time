# tablecloth.time


[![Clojars Project](https://img.shields.io/clojars/v/org.scicloj/tablecloth.time.svg)](https://clojars.org/org.scicloj/tablecloth.time)
[![build](https://github.com/scicloj/tablecloth.time/actions/workflows/cd.yml/badge.svg)](https://github.com/scicloj/tablecloth.time/actions/workflows/cd.yml)


## Description

This library offers tools for manipulating and processing time-series
data. It compliments and extends the easy-to-use API provided by
[`tablecloth`](https://github.com/scicloj/tablecloth) for working with
the highly performant columnar datasets of
[`tech.ml.dataset`](https://github.com/techascent/tech.ml.dataset).

## Usage

TBD

## Development

We use a "hybrid" Leinigen/tools.deps setup. You can use either `lein`
or `clj/clojure`. We chose this hybrid setup so that we get the
benefits of Leinigen's suite of build-related tools, while also
benefitting from the simplicity of tools.deps' approach to
dependencies. For more context on the differences and trade-offs
between the the two, see [this
post](https://clojureverse.org/t/is-there-a-sales-pitch-for-switching-to-deps-edn-from-lein-in-2020/5367/5).

#### Linting

We use both `cljfmt` and `clj-kondo` to lint our code. To run the linters, do:
```bash
lein lint
```

#### Tests

We run tests using the `midje` test runner, which will run both any midje tests
and any standard clojure tests:
```bash
lein midje
```

## Contributing

Development for this project happens in the SciCloj [fundamentals
study
group](https://scicloj.github.io/pages/web_meetings/#sci-fu_group), a
group focused on improving the Cojure datascience ecosystem. We tend
to hang out in the
[\#sci-fu](https://clojurians.zulipchat.com/#narrow/stream/265544-sci-fu)
stream on the Clojurians Zulip, and we meet regularly to coordinate,
learn, and solve problems.

We eagerly invite your participation in this project. The project is currently in an experimental phase and we are approaching its development interactively as a group in a way that is driven by openness and learning. If you are interested, please reach out.

Please peruse this project's [issues](https://github.com/scicloj/tablecloth.time/issues) to get a sense of work that is ongoing for this project.

## License

MIT for now, but this is basically a placeholder.  Open to suggestions.
