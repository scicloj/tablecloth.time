(ns tablecloth.examples.working-with-time-series-2
  (:require [tech.v3.datatype :as dtype]
            [tech.v3.datatype.datetime :as dtype-dt]
            [tech.v3.datatype.functional :as dtype-fn]
            [tech.v3.dataset :as dataset]
            [tablecloth.api :as tablecloth]
            [tablecloth.time.index :as idx]
            [tablecloth.time.api :as time]
            [tick.alpha.api :as t]
            ;; [clj-http.client :refer [get]]
            [notespace.api :as notespace]
            [notespace.kinds :as kind]
            [aerial.hanami.common :as hanami-common]
            [aerial.hanami.templates :as hanami-templates]))


["## Date and Times in Clojure"]

["TODO: Our opinionated way of wrapping dtype-next datetime and/or tick"]

["Things to cover:
   - a general parse method (e.g. t/parse) like np.datetime64() sorta.
   - a way to coerce a datetime to a different unit (like: np.datetime64('2015-07-04 12:59:59.50', 'ns'))
   - qualified keywords for datetime types/units"]

["Clojure is hosted on the Java JVM, so its time support is based on Java's `java.time` library.
This is an example of a Java time class:"]

(java.time.LocalDate/now)

["However, when we interact with time we will use a series of helper functions pprovided by `tablecloth.time`..."]



["## Typed Containers/Columns in CLJ"]

["TODO: Does the user need to think about type at this (tablecloth) layer??"]

["TODO: Will we have a equivalent to UFuncs for doing e.g. `date + np.arrange(12)`?"]

["TODO: int-64 encoding cause a limit to maximum time span?"]


["## Indexing by time"]

["When working with time, you will frequently want to specify a time-based column as an \"index\". But what is the point of specifying a column as an \"index\"?

Generally speaking, the value of an index is that it makes possible efficient access and subsetting of the dataset based on the column(s) values. An indexable column can consist of many types, but in some cases they must also be unique. For example, an alphabetical index can include repetetive strings, but an index of time values cannot repeat. [True?]

To specify a column as an index we call `index-by` on that column. Let's start by building some dummy data:
"]

(def n 100)

(defn random-datetime []
  (dtype-dt/milliseconds->datetime :local-date-time (* 1000 (rand-int 999999999))))

(def ds
  (-> {:datetime (dtype/make-reader :local-date-time n (random-datetime))
       :x        (repeatedly n rand)}
      tablecloth/dataset
      (tablecloth/order-by :datetime)
      (tablecloth/add-or-replace-column :i (dtype/make-reader :int64 n idx))))

^kind/dataset
ds

["Now let's add an index to the dataset `ds`:"]

(def indexed-ds (idx/index-by ds :datetime))

^kind/dataset
indexed-ds

["TODO: Eventually, this ☝️ will print some indication of the index."]

["TODO: Python also includes data structures for:
   - time periods
   - time durations"]


["## Resampling, Shifting and Windowing"]

(def raw-ds
  (-> "https://raw.githubusercontent.com/techascent/tech.ml.dataset/master/test/data/stocks.csv"
      (tablecloth/dataset {:key-fn keyword})))

(tablecloth/head raw-ds)

(tablecloth/shape raw-ds)

(defn prep-data-for-plotting [ds date-col-key cols-to-drop]
  (-> ds
      (tablecloth/drop-columns cols-to-drop)
      (dataset/column-cast date-col-key :string)
      (tablecloth/rows :as-maps)))

(def ds-2005-2016
  (-> raw-ds
      (tablecloth/select-rows #(= "MSFT" (:symbol %)))
      (idx/index-by :date)
      (time/slice "2005-01-01" "2016-12-01")))

(tablecloth/shape ds-2005-2016)

(tablecloth/head ds-2005-2016)

^kind/vega
(hanami-common/xform
 hanami-templates/line-chart
 :DATA (prep-data-for-plotting ds-2005-2016 :date [:symbol])
 :X :date
 :XTYPE :temporal
 :Y :price
 :YTYPE :quantitative)

(-> ds-2005-2016
    (tablecloth/group-by #(-> % :date t/year))
    (vary-meta assoc :print-line-policy :repl))

(-> ds-2005-2016
    (tablecloth/group-by #(-> % :date t/year))
    (tablecloth/aggregate (fn [group-ds]
                            {:symbol (-> group-ds :symbol first)
                             :price (dtype-fn/mean (:price group-ds))}))
    (tablecloth/rename-columns {:$group-name :year
                                :summary-symbol :symbol
                                :summary-price :price}))

(def ds-2005-2016-yearly
  (-> ds-2005-2016
      (tablecloth/group-by #(-> % :date t/year))
      (tablecloth/aggregate (fn [group-ds]
                              {:symbol (-> group-ds :symbol first)
                               :price (dtype-fn/mean (:price group-ds))}))
      (tablecloth/rename-columns {:$group-name :year
                                  :summary-symbol :symbol
                                  :summary-price :price})))

^kind/dataset
ds-2005-2016-yearly

(-> ds-2005-2016-yearly
    (tablecloth/drop-columns [:symbol])
    (dataset/column-cast :year :string)
    (tablecloth/rows :as-maps))

^kind/vega
(hanami-common/xform
 hanami-templates/line-chart
 :DATA (-> ds-2005-2016-yearly
           (tablecloth/drop-columns [:symbol])
           (dataset/column-cast :year :string)
           (tablecloth/rows :as-maps))
 :X :year
 :XTYPE :temporal
 :Y :price
 :YTYPE :quantitative)
