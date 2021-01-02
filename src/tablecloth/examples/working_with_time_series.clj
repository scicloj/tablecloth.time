(ns tablecloth.examples.working-with-time-series
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as dtype-dt]
            [tech.v3.datatype.functional :as dtype-fn]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as idx]
            [tick.alpha.api :as t]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]))

["## Dates and Times in Clojure

Clojure is hosted on the Java JVM, so it's time support is based on Java's `java.time` library.
This is an example of a Java time class:"]

(java.time.LocalDate/now)

["Clojure has libraries that wrap the Java time classes to make working with time more fluent. We'll focus on a library called `tick` that provides a useful set of functions for manipulating
java.time classes.

Here's how you create a time object using `tick`:"]

(t/new-time 23 59 59 999999)

["Here's how you parse a date from a string:"]

(t/parse "2010-10-10")

["Tick doesn't yet handle all string formats. In that case, we can use Java's classes:"]

(java.time.LocalDate/parse
 "Tue, 3 Jun 2008 11:05:30 GMT"
 java.time.format.DateTimeFormatter/RFC_1123_DATE_TIME)

["If you have a datetime, you can do other things with it, like discover the day of the week."]

(-> (t/parse "2010-10-10")
    (t/day-of-week))

["## Typed arrays of time

In Python, you can build numpy arrays, where the array has the defined type as a datetime:

```
import numpy as np
date = np.array('2015-07-04', dtype=np.datetime64)
#=> array(datetime.date(2015, 7, 4), dtype='datetime64[D]')
```

As discussed in Chapter 2, we less frequently interact directly with arrays in Clojure, instead operating on datasets. However, we can use dtype-next's containers to build typed collections of datetime data."]

(dtype/make-container :local-date [(tech.v3.datatype.datetime/local-date)])

["Python also makes it possible to work with a numpy array that is typed with vectorized computations like this:
```
np.array('2015-07-04', dtype=np.datetime64) + np.arange(12)
#=> array(['2015-07-04', '2015-07-05', '2015-07-06', '2015-07-07',
       '2015-07-08', '2015-07-09', '2015-07-10', '2015-07-11',
       '2015-07-12', '2015-07-13', '2015-07-14', '2015-07-15'], dtype='datetime64[D]')
```

In Clojure, this is not supported right now, but we do have the ability to add time at a specific
interval.
"]

(dtype-dt/plus-temporal-amount (dtype-dt/local-date) (range 5) :days)
(dtype-dt/plus-temporal-amount (dtype-dt/local-date) (range 5) :months)
