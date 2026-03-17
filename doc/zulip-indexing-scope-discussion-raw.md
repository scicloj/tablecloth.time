# Zulip Discussion: Index Structures in Columns - Scope

_Source: Clojurians Zulip #tech.ml.dataset.dev > "index structures in Columns - scope" thread_
_Saved: 2026-03-17_

This is a follow-up thread clarifying the scope of the index structures project.

---

**Daniel Slutsky:** Hi @Will Cohen! Great discussion at #tech.ml.dataset.dev > index structures in Columns. :praise: I'm opening this separate topic thread to clarify the scope of the index structures project (don't want to disrupt the discussion going on on that other thread). If I understand correctly, the current focus is geospatial index structures. Recently, two related needs have been raised back:

1. **Categorical index (lookup table).** @Epidiah Ravachol is writing tutorials that compare Pandas to Tablecloth and is looking for idiomatic ways to mimic Pandas index behaviour. Quoting the Python Data Science Handbook:
```python
import pandas as pd
data = pd.Series([0.25, 0.5, 0.75, 1.0], index=['a', 'b', 'c', 'd'])
data['b']
```

2. **Ordinal index (some tree structure), e.g. time index.** @Ethan Miller is coming back to the tablecloth.time project, that used to rely on the index support that has been removed from tech.ml.dataset. We will need some support for time-based index structures.

Possibly, after a proof-of-concept with the geospatial index structures, it'd make sense to consider a general library to support a general index and follow similar principles. What do you think?

---

## Harold's Categorical Index Example

**Harold:** @Daniel Slutsky - thanks for this topic, I like the way you've presented these ideas here. I'd love to discuss more "idiomatic ways to mimic Pandas index behaviour." A pd.Series has a lot in common with a TMD Column. Here's a not-so-great idea to maybe get this conversation started in the spirit of Stone Soup:

```clojure
user> (require '[tech.v3.dataset :as ds])
nil
user> (def ds (ds/->>dataset {:x (map #(/ (inc %) 4) (range 4))}))
#'user/ds
user> ds
_unnamed [4 1]:
|       :x |
|----------|
|   0.2500 |
|   0.5000 |
|   0.7500 |
|        1 |
user> (:x ds)
#tech.v3.dataset.column<object>[4]
:x
[0.2500, 0.5000, 0.7500, 1]
user> (def index (zipmap ["a" "b" "c" "d"] (range)))
#'user/index
user> index
{"a" 0, "b" 1, "c" 2, "d" 3}
user> (get (:x ds) (get index "b"))
1/2
```

---

## Pandas Index Type Inconsistency (Harold's Discovery)

**Harold:** First, in researching this, I found an inconsistency in pandas indexing; **the type returned by a lookup can vary based on the number of elements that match a given index value (!)** - this seems not ideal to me, as downstream code looking things up by index needs to account for this.

```python
>>> import pandas as pd
>>> s = pd.Series(range(3), index=list("aab"))
>>> s.loc["a"]
a    0
a    1
dtype: int64
>>> type(s.loc["a"])
<class 'pandas.core.series.Series'>
>>> s.loc["b"]
2
>>> type(s.loc["b"])
<class 'numpy.int64'>
>>> # odd (to me): same operation returning different types
```

---

## Functional Index Pattern (Harold)

**Harold:** Here's my next thought (admittedly, only a small improvement):

```clojure
user> (defn obtain-index [ds colname]
  (let [m (ds/group-by-column->indexes ds colname)]
    (fn [v]
      (map (partial ds/row-at ds) (get m v)))))
#'user/obtain-index
user> (def lookup (obtain-index ds :id))
#'user/lookup
user> (lookup "a")
({:id "a", :x 0.6041307098079549, :y 0.5595916638050421}
 {:id "a", :x 0.5480122743107274, :y 0.2408579756029523})
```

This has some very nice properties:
- The returned index function is closed over the dataset value
- When looked up, the sequences of rows are lazily realized
- The row data is served up directly from the dataset, without copying
- Any number of these kinds of indexes can coexist on a dataset without using any extra ram

---

## Ethan's Context on tablecloth.time

**Ethan Miller:** Hi all, I'm sorry I haven't jumped into this conversation more yet. I had implemented an index within tech.ml.dataset some time back and it was removed during a process of simplifying tech.ml.dataset. What I'm working on now is to extract that work into a self-contained library -- tablecloth.index -- that as I am thinking about it now could be included with tablecloth in dependencies in order to expand the functionality of tech.ml.dataset in that direction.

I need to reengage a bit with that code and how we used it in tablecloth.time -- an experimental time series addon to tablecloth -- in order to reconsider from the distance of time-passed how that approach feels now. My recollection is that the way we focused on it then -- from the perspective of tablecloth.time -- was largely to hide the implementation. We were building higher order functions that would use the index api but hide it from the user. That fit with the "level" of tablecloth as a lib that hid the "closer to the metal" approach of tech.ml.dataset.

The conversation above seemed more about interacting directly with the index during data analysis, an approach that may make more sense now from tablecloth's perspective given that we now have the Column api in tablecloth.

I'd be interested to see instances maybe in the #real-world-data conversation of when/how the need for the index arises. **(I've never liked the Pandas index api).**

---

## Index Complexity Discussion

**jsa:** @Harold that's very nice. I guess the question is, what is the complexity of ds/group-by-column->indexes?

**Ethan Miller:** We actually know this from an analysis that @Daniel Slutsky did some time ago, before we built the index implementation in tmd that was removed. group-by-column->indexes is based on the index-space operations in dtype-next, which are I think parallelized. Still in Daniel's analysis we saw a significant performance boost with TreeMap for ranged rather than categorical lookup.

**Harold:** That's a good question. It's O(n), and that is optimal, since it needs to look once at each row of the dataset. However, naturally, you build that structure once and then query it many, many times.

---

## Design Philosophy Discussion

**Ethan Miller:** I've been wondering about the decision to remove the index from tmd way back when, and what the engineering/design considerations were. Do you have any insight into that? The reason I'm asking is that I wonder if there's some general design ethos considerations that might be relevant when thinking about TMD's feature set and how we should think about things when we feel the impulse to expand on that feature set.

**Harold:** I don't have any special insight there - I wasn't involved. I think indexing in particular is a bit of a sprawling topic, and there isn't yet a great consensus on what to do for it from a functional data science perspective. **I have been reading Pandas' source in this area, and it's a bit bonkers. I would estimate there is about as much code in Pandas doing indexing stuff as there is _all of the code in TMD_**, and every time I poke around in Pandas I find something demented, or at least surprising.

**Harold:** "..., and then expect other features to be added outside that core..." I think this is right. It's a strength of Clojure that so much can be done from the outside. **Our policy is always to implement the first version of something inside a specific leaf (client) project that needs it. Then, if that thing is later needed elsewhere, it can be lifted into a library that those projects share.**

**Harold:** **What I'd really like to see is examples of powerful uses of indexing in Pandas that lead to enviable solutions to actual problems. Then we can look at those and see what features they imply we need.**

---

## The Key Insight: Binary Search Over Trees

**Chris Nuernberger:** **You only need tree structures if you are adding values ad-hoc or removing them - usually with datasets we aren't adding/removing rows but rebuilding the index all at once. Just sorting the dataset and using binary search will outperform most/all tree structures in this scenario as it is faster to sort than to construct trees. Binary search performs as well as any tree search for queries and range queries.**

**Daniel Slutsky:** Oh that makes sense!

---

## Real-World Performance Validation (Harold)

**Harold:** @Ethan Miller - thought of you today, a real-world example of a temporal dataset indexing problem arose at work. [Download the test dataset here: t-ds.nippy]

This dataset has a :t column of java.time.Instants and an :n column of doubles, and 1M rows. I would like to compute (efficiently), for each row, the index of the row closest to one week later.

**Ethan Miller:** Cool problem @Harold. Off hand that kind of problem doesn't specifically relate to the time series related work that I had done with others... Now based on comments above from you and Chris I realize that may not be the case, and **we can rely on some techniques using dtype's efficient argops and binary search.**

More concretely what we got up to in tablecloth.time actually revolves around some very basic functions:
- A **slice function** that worked with the index structure for segmenting time series data
- A **rolling average function**
- **Time manipulation abstractions** that took advantage of tech.ml.dataset/dtype-next's clear time conversion pathway through conversion to milliseconds

**Harold:** **I'm actually getting surprisingly good performance out of java.util.Collections/binarySearch + tech.v3.dataset/row-map, I can decorate the example dataset above in less than 1s (and the real-world dataset that has more than 1M rows also at a rate greater than 1M rows/s).**

---

## Daniel Slutsky's Multiple Values Search Algorithm Suggestion

**Daniel Slutsky:** One approach that a couple of papers propose is to run all searches together and let them learn from each other.
- https://www.researchgate.net/publication/254560725_Multiple_Values_Search_Algorithm
- https://github.com/juliusmilan/multi_value_binary_search

We are searching for a sorted list of values (t+week in our case) in a sorted list of values (t in our case). Multiple binary searchers for multiple sorted values maintain many ranges, gradually narrow them by bisection, and collapse them due to their relationships.

---

_End of transcript_
