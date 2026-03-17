# Zulip Discussion: Index Structures in Columns - Raw Transcript

_Source: Clojurians Zulip #tech.ml.dataset.dev > "index structures in Columns - scope" thread_
_Saved: 2026-03-17_

This is the raw conversation transcript. For a summarized version with key takeaways, see `zulip-indexing-discussion-summary.md`.

---

**Daniel Slutsky:** Slicing by Column data seems to be a common need. If we do it a lot, it makes sense to support it using some auxiliary data structure. As we have recently demonstrated, in some situations this matters a lot. The index structure may be: a java.util.HashMap for categorical columns a java.util.TreeMap (or a sorted array that we can binary search on) for numerical, date and time columns an org.locationtech.jts.index.strtree.STRtree (or another spatial index) for geometrical/geographical columns. The index is considered valid for a specific Column value. If a Column is transformed somehow, resulting in a new value, then the index should be considered irrelevant. We have been experimenting with different ways of holding the relevant index structures: metadata, context maps, etc. James also suggested holding them in some dedicated atom. All these may work, but do bring up some questions, such as cleaning up obsolete index structures. Recently, we started thinking about the tmd Column as a user-facing concept, that may take an important part of in the emerging user-friendly experience. This seems to encourage the following idea: maybe the index structure should be part of the Column concept. I think this will make it easier to manage. For example, when the Column is garbage collected, the index structure is garbage collected too. Also, there is no risk of mistakenly using an index structure with a transformed version of the Column it indexes -- the validation problem becomes trivial. Here is one way to make it work: The PColumn protocol will have an additional index function, that returns the associated index structure. The Column implementation will compute this structure lazily, on the first tall to the function. The definition of this computation will be extensible -- we will be able to handle types that dtype-next is not aware of (e.g., geometries) with appropriate index structures. Various data processing functions can rely on this structure if they wish to. Possibly, all index structures will be wrapped by some Clojure code, so that they may implement some common protocol which is handy for slicing (TBD). @sid @Ethan Miller @James Tolton @Chris Nuernberger @generateme What do you think?

**generateme:** I think the idea with index inside Column type is ok. I like it. If column is transformed, new one is created and old index vanishes (though, there are operations which should copy index, like set-name). Still there can be issues with missing values and cached-vector field which is mutable.

**Daniel Slutsky:** Thanks. I hope to understand what you said about missing values and cached-vector. Anyway, we should check all the Column API to make sure that we are not missing out on any edge cases. :smile:

**generateme:** (I hope to understand too :smile: - it's a gut feeling) These are two additional structures in Column type. We need to analyze interactions between them and index concept. What happens when - as you mentioned, these can be edge cases.

**James Tolton:** Sounds like the most robust approach so far, and the one that contains the least number of surprises for users and library authors!

**Daniel Slutsky:** @Chris Nuernberger what do you think?

**Chris Nuernberger:** I agree with @James Tolton - this approach seems robust. There shouldn't be any edge cases really; the column's data in cached-vector is constant - there are no public API's to change that data and adding another cached structure especially one derived from the data makes sense. One question I might have would be what happens when the column is cloned or what should happen upon selecting a subset of the indexes - probably the index structure should just disappear in most cases. I think that the column maybe doesn't know enough about its data in all cases, primitive types for instance, to calculate the index so perhaps a keyword or more information indicating the calculation mechanism will have to be saved as metadata on the column. It can then be calculated on demand via a multimethod. I think we have to try it. Perhaps storing the index in the metadata of the column is equivalent but cheaper in terms of developer overhead but then it might get copied to a new column so the column would then have to be aware of the keys that might store index information which is a bit of breach of the metadata contract. Its a toss up :-).

**Chris Nuernberger:** You have both index selection in terms of select and concatenation in terms of concat-columns. In both cases I would expect index information to disappear in general and need to be recalculated.

**Daniel Slutsky:** @Chris Nuernberger Thanks! Then @Ethan Miller and I will create a PR if that is ok. "what happens when the column is cloned or what should happen upon selecting a subset of the indexes - probably the index structure should just disappear in most cases." Yes, we went over all calls to the (Column. ...), and mapped those where the index structure needs to be replaced with nil. How to handle missing values is indeed still a question, as @generateme anticipated. "so perhaps a keyword or more information indicating the calculation mechanism will have to be saved as metadata on the column." I think we haven't run into such cases yet, but it makes sense to support that. For example, a geography user may wish to try different types of spatial index structures.

**joinr:** @Daniel Slutsky Why not have an "indexed column" type that just wraps the existing column implementation, delegates all the normal methods to it, but maintains the (user-defined) index structure and implements an IndxedColumn protocol or something equivalent that can be accreted? This is a common way of getting psuedo-inheritance via composition (let the t.m.d. column do the work, except in places where you'd delegate to using the index). One could probably define a simple API to define the column index (e.g. (index-column the-dataset :blah tree-map), which would compute the derived column and assoc it into the dataset using the existing t.m.d. functionality (or even just assoc). It's a small variation on your proposal but makes the changes a) accretive, the underlying concept of indices are not fundamental to columns, b) extensible - users can define their own means of indexing.

**Daniel Slutsky:** @joinr thanks. One thing I like about this option is that it will not complicate the existing code of the Column datatype further, but rather live as a separate piece of code, which is maybe easier to reason about. Can you explain how it helps with extensibility?

**generateme:** If you implement usage of the index in terms of java interface or clojure protocol, index can be anything, not only a TreeMap

**joinr:** Yeah, I would imagine something like an Index protocol. I think I remember something like this from the design in thi.ng.

**joinr:** @Daniel Slutsky The downside is you add a new type, and there's a little boiler plate in wrapping the underlying column protocols, seq, iterator, reader, etc (no big deal, pretty much a copy-and paste that delegates - could be done with a macro but probably not worth it for a 1-time implementation). If the protocols change (I think they are stable at this point), you'd have to update the indexed column type as well. If anything, you could prototype the indexed column approach separately and see how it works out. I'm curious if there are use cases for multi-column indices and how those could be handled. I imagine with a Index protocol, you could accomplish some kind of shared index and supply that to the columns in question. Don't know if this is even a concern or if indices are always column-specific. I'm weak on database stuff these days.

**Daniel Slutsky:** "If you implement usage of the index in terms of java interface or clojure protocol, index can be anything, not only a TreeMap" Yes, nice. We are still looking into our various use cases (indexing of categories, of linearly ordered things, of geometries), to see if such generalization makes sense and how.

**Daniel Slutsky:** "The downside is you add a new type..." Thanks

**Daniel Slutsky:** "I'm curious if there are use cases for multi-column indices..." In case we need that, we may need an analogous plan for the Dataset datatype. One example of such need: we have a dataset with daily temperatures per city, and want all the measurements at September 2019 for all cities which are close to Helsinki.

**Daniel Slutsky:** "Can you explain how it helps with extensibility?" I still didn't get why an additional IndexedColumn type makes the situation more extensible.

**joinr:** The added column type makes the implementation simpler, accretive. The extensibility comes from how the index is implemented. If you embedded the index into the existing column type, you could do the same thing so long as the index was implemented on top of protocols or some means for a user to define "how" to index the column.

**Daniel Slutsky:** :+1: :blush:

**joinr:** It may end up that slipping the index into Column is easy enough as well; just design considerations.

**Daniel Slutsky:** Thanks. So far our draft has been feeling easy. We may still decide to extract it, as you suggested. The doubts are indeed around missing values and extensibility.

**James Tolton:** "Why not have an 'indexed column' type..." Do you know of any examples of this technique anywhere? I feel like this would be hugely useful if there was a way to do it without copy/paste

**James Tolton:** (this is badly needed in libapl-clj, for example)

**Ethan Miller:** @James Tolton Daniel and I have been working on a PR that we'll open a draft of at some point soon to get input.

**James Tolton:** w00t! I love input!

**joinr:** @James Tolton I usually do it with copy / paste or simple macros, however there are examples like delegating-deftype that are more formal. deltype looks more formal since it appears to scrape protocols and interfaces vua reflection, but I'm not well versed on it.

**generateme:** You can also define own protocols (if there is pure enhancement) and apply extend-type on Column with custom protocol implementation.

**generateme:** "I usually do it with copy / paste or simple macros..." Wow, I was looking for delegating-deftype.

**James Tolton:** ooo ... depending on how well that works this could be a whole new fun pile of land mines!

**generateme:** haha, indeed

**joinr:** There are some methods that create new instances, so pure delegation can't really be automated (like conj, assoc). You have to wrap those, which is what deltype does.

---

## Key Turning Point: Index Removal from TMD

**Chris Nuernberger:** @Ethan Miller @Daniel Slutsky - Hey, I am going to remove the index column pathways from dataset for now. I haven't seen any users using this pathway and I am trying to simplify the column system.

**Ethan Miller:** Oh interesting. That would affect some of the work I was hoping to do in the tablecloth.time that I just haven't gotten to yet.

**Ethan Miller:** Hmmm, yeah I'm wondering if that's necessary. I think it can still be a very useful feature that I think we demonstrated offered some speed benefits in some cases.

**Ethan Miller:** Even though it needs more development, I think tablecloth.time actually is a user of that feature and demonstrates how it can be powerful/helpful. Some of the pathways that it provides though not in heavy use by the community right now, I think could have some uptake when that library is built out more, and it something that I'd really like to do. I've been quietly stashing away instances of people asking questions about time data processing so that I can work from actual cases.

**Ethan Miller:** Here is an example fn already implemented in tablecloth.time that can slice a dataset using the index structure: https://github.com/scicloj/tablecloth.time/blob/main/src/tablecloth/time/api/slice.clj#L13-L76. It uses a set of utility functions that make use of the index here: https://github.com/scicloj/tablecloth.time/blob/main/src/tablecloth/time/utils/indexing.clj

**Ethan Miller:** Also here is a demo from part of Sami and my talk from last year's reClojure of a different feature of tablecloth.time, also using the index structure: https://youtu.be/RGMGyEY5RK4?t=1121

**Chris Nuernberger:** Sure, then move that code to the tablecloth time library.

---

## The Key Insight: Do We Need an Index at All?

**Ethan Miller:** Okay, I'll try that if it seems to make sense. I wonder -- on a more general level -- I would have thought that an index was somehow necessary, but maybe there's something here to learn about the properties of tech.ml.dataset? **Instead of the index structure, is there some other method or under-the-hood property of the library that makes this kind of optimization is not necessary? Maybe I don't need an index structure at all...?** I guess that's what you are saying when you say that there are no users of this feature...is that right?

**generateme:** I think an index structure and index operations can be external. You can expose functions for index creation and make slice to expect index as a parameter. The second option is to create a protocol and extend Column datatype outside TMD or TC.

**Chris Nuernberger:** **There is nothing under the hood aside from careful engineering to make the linear case very very fast.** Aside from that users are building their own indexes external the the library such as various functions from input->index range. So they aren't using the index system but engineering an ad-hoc system to speed up whatever query they need to when they need it.

**Chris Nuernberger:** Again, for the index system to be generally useful it would have to be integrated with more of the other API calls and that is something we can revisit later.

**Ethan Miller:** "There is nothing under the hood aside from careful engineering to make the linear case very very fast..." Thanks. This context about how people are using the tool helps me understand.

**Ethan Miller:** "Again, for the index system to be generally useful..." Yes this makes sense too, and I see how the index is just dead weight a bit without this. I know this was the original intention that we'd discussed (index-aware functions) and I'd hope to work on it, but got pulled into this both column project for TC.

---

## Spatial Indexing Discussion (Will Cohen)

**Will Cohen:** Hi all — where is the best place to re-pick up discussion of indexes? Re-visiting now with JTS objects in a column and trying to figure out how to play around with generating them for a TMD column. As best as I can tell — this has moved to tablecloth, is that right?

**Will Cohen:** In this case, JTS would make an tree object that can basically take a query for a geographic rectangle and return all objects whose own rectangular envelope intersects with that, which is way faster than a full GIS intersection test, and dramatically trims the rows where the slow path needs to run. (https://locationtech.github.io/jts/javadoc/org/locationtech/jts/index/hprtree/HPRtree.html seems like the best one now and also can be updated without full regeneration.) To avoid duplicating all JTS objects into the index I think I'd want it to store row numbers as the indexed values, which can then be used to refer to the JTS object in the actual column.

**Will Cohen:** In an ideal world whenever the column changes there'd be some kind of hook to update the index accordingly, but I assume there's a lot of thinking to do before that.

**Will Cohen:** @Daniel Slutsky additionally, if neither tablecloth nor TMD are the right place to stash an index anymore, is there some kind of type to use to add a "row object" to a protocol?

**Daniel Slutsky:** I think it is a good place and a good timing to think about those index structures. Since it was decided to leave them out of tech.ml.dataset, we should find a way to add such details on the user side. I think we can implement a proof-of-concept by holding the index in a Column's metadata. Later, we can discuss whether to prefer another implementation. We have at least three kinds of index structures in mind: an index for equality-based join operations - a simple map an index for temporal join operations - a tree of intervals an index for spatial join operations - a tree of rectangles We do not have to address all of that together -- let us start with the spatial index? I don't know whether it should be part of Tablecloth. @generateme What do you think?

---

## The Community Consensus: External Indexes

**Harold:** "I think it makes sense to first implement that on the user side, on top of tech.ml.dataset" This strikes me as correct. A lot of what people want when they say 'indexes' boils down to a few auxiliary structures that can be used to enable faster algorithms than what TMD does by default. Doing that is of course interesting, secondary is finding some way to bolt those structures onto a dataset (or elsewhere) and try to enable those faster algorithms automatically (typically quite challenging to do in any kind of universal/general way).

**Will Cohen:** Lovely. I'll get to poking around with metadata and see how it goes. Thank you all.

**generateme:** Let me add some 2cc. As Harold said it's challenging to create general solution to allow use of indexes by TMD/TC. I think the layer with specialized indexes and functions relying on them would be an easiest path. I mean something like this: let user create an index and apply a function which understand it. Both: index creation and respective functions can live outside (but on the top of) TMD/TC.

**generateme:** Here is a PoC of such idea from the past: Using TreeMap index to subselect datasets (github.com)

**Daniel Slutsky:** BTW in the R sf package, geographical index creation is done implicitly without user intervention. https://r-spatial.org/r/2017/06/22/spatial-index.html

---

## Harold's Practical Wisdom

**Harold:** Others will have different opinions, but I think what you're describing sounds right, and matches a pattern we have used relentlessly. An in-memory index in an atom (together with functions that can reconstitute it) and query patterns that know to check the index first, sounds like a great place to start. Unbounded opportunities to spiral off into partial reconstitution of the index, or write through stuff if updates are a problem, and so on and so on. **Main mistake to avoid is over-engineering any of those parts. Start by indexing nothing (!) - let painful query performance tell you what indexes you need. I am surprised on every project how far we get with no smarts at all in queries and searching - then much later some unexpected thing turns out to be the bottleneck, and we slap an index on it, and never hear from any of it ever again.**

Recent real-life comment from a live project:
```clojure
;; TODO: HH 2024-07-31 - This is O(m*n), if that ever matters, then we can could pre-filter by :foo/bar [actual keyword redacted]
```

---

## Will Cohen's Spatial Index Implementation

**Will Cohen:** @Harold -- actually, possible tweak. Realizing that people may not always have datasets def'd in a particular place. Rather than attaching the big index to the ds, thoughts on having something similar to set-dataset-name that adds some kind of internal UUID sort of unique identifier to a :spatial-index-id metadata, which then can be used to cross reference in this atom of indexes?

**Harold:** I'm not sure I follow - but the implementation of set-dataset-name is nothing special (it's a one-liner): https://github.com/techascent/tech.ml.dataset/blob/48002dfec3eeb7f7b835599b802093a5be37a1ba/src/tech/v3/dataset/base.clj#L52-L54 You can add arbitrary metadata in the same way, if you like.

**Will Cohen:** [Shows example of spatial dataset with UUID-based index lookup]

**Will Cohen:** This actually will work out well -- Since a column can be any old java object, I think I can actually have the spatial operations work on Featurelikes...

---

_End of transcript_
