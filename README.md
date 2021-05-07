# tablecloth.time

> Note: This library is in active development and is not ready for general use.

## Description

This library provides tools for performing time-series analyses. It is built upon
the foundation of [`scicloj/tablecloth`]() in that:

* It assumes that in most cases people who use this library will use tablecloth's API
to manipulate datasets;
* Its goal is to provide functional tools related specifically to time-series analysis
that are not covered by tablecloth itself; and
* Its syntax and vernacular matches that of tablecloth, so that the functional signatures
of this library should be familiar if you are accustomed to using tablecloth.

## Development

#### Linting

We use both `cljfmt` and `clj-kondo` to lint our code. To run the linters, do:
```bash
> lein lint
```

#### Tests

To run the tests, do:
```bash
> lein test
```

## License

MIT for now, but this is basically a placeholder.  Open to suggestions.
