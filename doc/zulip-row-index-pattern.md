# Zulip Discussion: Row Index Pattern

_Source: Clojurians Zulip_
_Date: October 11, 2021_
_Saved: 2026-03-17_

A quick exchange showing the common pattern for adding row indexes in TMD — users build what they need.

---

**Awb99:** Does TML dataset build internally an index of the rows in a dataset? For testing, in huge datasets, I sometimes need to look at a specific row, to investigate if I am doing the right things. Currently I manually add an index column to each tml dataset. So that later on I can query it by row id. But I am pretty sure that this index is already somewhere available? Or not? Thanks!!

**Chris Nuernberger:** 'rows' returns an object with an efficient nth implementation.

**Awb99:** I get that. But say I am iterating with a mapseq reader. Then I have say a row that matches a criteria, and then I want to add this to a new dataset as and set the column :matched-row to the current index of the row. In the mapseq reader I dont see the IDs. So how do I get them? I would have to create them at some point.

**Chris Nuernberger:** I think you want the original column indexes in your filtered dataset. **In that case adding a column from (range n-rows) then filter result will be efficient way to do this. Result smaller ds will have row indexes mapping back.**

**Awb99:** yes. this is what I do:
```clojure
(defn add-running-index [ds]
  (tablecloth/add-column ds :index (range 1 (inc (tablecloth/row-count ds)))))
```

**Awb99:** techml dataset as a vectorized relational database 🙂 ha ha ha

**Chris Nuernberger:** Yes, precisely :-). Nice solution.

---

## Takeaway

This illustrates the pattern: TMD doesn't have built-in row IDs, but adding an index column via `(range n-rows)` is trivial and efficient. Users build what they need, explicitly.
