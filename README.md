![](https://github.com/scicloj/tablecloth.time/actions/workflows/cd.yml/badge.svg)


## Description

This library provides tools for performing time-series analyses in Clojure. It builds
on the dataset API provided by the [`tablecloth`](https://github.com/scicloj/tablecloth)
library. Specifically, that means that:

* It assumes that in most cases people who use this library will use tablecloth's API
to manipulate datasets;
* It provides functional tools related specifically to time-series analysis
that are not covered by tablecloth itself; and
* Its syntax matches tablecloth's, so that the functional signatures
of this library should be familiar if you are accustomed to tablecloth.

## Development

This library uses `lein-tools-deps` so we can have a "hybrid" Leinigen/tools.deps
setup. That means you can use either `lein` or `clj/clojure`. We chose this hybrid
setup so that we get the benefits of Leinigen's full suite of build-related tools,
while also benefitting from the simplicity of tools.deps' approach to dependencies.
For more context on the differences and trade-offs between the the two, see [this post](https://clojureverse.org/t/is-there-a-sales-pitch-for-switching-to-deps-edn-from-lein-in-2020/5367/5).

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

Development for this project happens in the SciCloj [fundamentals study group](https://scicloj.github.io/pages/web_meetings/#sci-fu_group), a group focused on improving the Cojure datascience ecosystem. We tend to hang out in the [\#sci-fu](https://clojurians.zulipchat.com/#narrow/stream/265544-sci-fu) stream on the Clojurians Zulip, and we meet regularly to coordinate, learn, and solve problems.

We eagerly invite your participation in this project. The project is currently in an experimental phase and we are approaching its development interactively as a group in a way that is driven by openness and learning. If you are interested, please reach out.

Please peruse our this project's [issues](https://github.com/scicloj/tablecloth.time/issues) to get a sense of work that is ongoing for this project.

## License

MIT for now, but this is basically a placeholder.  Open to suggestions.
