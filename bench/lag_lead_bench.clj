;; Lag/lead benchmark: old tablecloth.time implementation vs. the current one
;; vs. Tablecloth 8.024's rolling-based `tc/lag`.
;;
;; Run from the repo root: clj -M:dev bench/lag_lead_bench.clj
;;
;; Tablecloth 8.x needs TMD 8, but this repo pins TMD 7.062, so `rolling-lag`
;; reproduces `tc/lag`'s body verbatim on top of TMD 7.062's rolling API.
(ns lag-lead-bench
  (:require [tablecloth.api :as tc]
            [tablecloth.column.api :as tcc]
            [tablecloth.time.api :as tct]
            [tech.v3.datatype :as dtype]
            [tech.v3.datatype.base :as dtype-base]
            [tech.v3.dataset.rolling :as rolling])
  (:import [java.time LocalDate]))

(defn old-lag
  "tablecloth.time's lag before PER-99."
  [col k]
  (let [col (if (tcc/column? col) col (tcc/column col))
        n (dtype/ecount col)
        values (dtype-base/select col (range 0 (- n k)))]
    (tcc/column (concat (repeat k nil) values))))

(defn old-add-lag [ds source-col k target-col]
  (tc/add-column ds target-col (old-lag (ds source-col) k)))

(defn rolling-lag
  "Body of tablecloth 8.024 `tc/lag`."
  [ds new-column-name column-name lag-size]
  (rolling/rolling ds
                   {:window-type :fixed
                    :window-size (inc lag-size)
                    :relative-window-position :left
                    :edge-mode :zero}
                   {new-column-name (rolling/first column-name)}))

(def impls
  {:old (fn [ds k] (old-add-lag ds :x k :l))
   :new (fn [ds k] (tct/add-lag ds :x k :l))
   :new-fill (fn [ds k] (tct/add-lag ds :x k :l {:fill (first (ds :x))}))
   :tc-rolling (fn [ds k] (rolling-lag ds :l :x k))})

(defn make-ds [datatype n]
  (let [rng (java.util.Random. 42)]
    (tc/dataset
     {:x (case datatype
           :float64 (double-array (repeatedly n #(.nextDouble rng)))
           :int64 (long-array (repeatedly n #(.nextLong rng)))
           :string (mapv #(str "s" (mod % 997)) (range n))
           :local-date (mapv #(.plusDays (LocalDate/of 2000 1 1) (long (mod % 9000)))
                             (range n)))})))

(defn- force! [ds]
  (let [c (ds :l)]
    (dtype/ecount c)
    (nth c (dec (dtype/ecount c)))))

(defn- time-ms [f]
  (let [t0 (System/nanoTime)]
    (force! (f))
    (/ (- (System/nanoTime) t0) 1e6)))

(defn measure
  "Warm up for at least `warmup` calls and 1 s, then time `runs` calls."
  [f {:keys [warmup runs]}]
  (let [deadline (+ (System/nanoTime) 1e9)]
    (loop [i 0]
      (when (or (< i warmup) (< (System/nanoTime) deadline))
        (force! (f))
        (recur (inc i)))))
  (System/gc)
  (let [ts (vec (sort (repeatedly runs #(time-ms f))))]
    {:median (ts (quot runs 2)) :min (first ts) :max (peek ts)}))

(defn- parity [datatype]
  (let [ds (make-ds datatype 50)
        before (((:old impls) ds 3) :l)
        after (((:new impls) ds 3) :l)]
    (and (= (vec before) (vec after))
         (= (vec (tcc/missing before)) (vec (tcc/missing after)))
         (= (dtype/elemwise-datatype before) (dtype/elemwise-datatype after)))))

(defn -main []
  (println "JVM" (System/getProperty "java.version")
           "| max heap MB" (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024))
           "| cores" (.availableProcessors (Runtime/getRuntime)))
  (println "Parity old vs new (values, missing, datatype):"
           (into {} (map (juxt identity parity) [:float64 :int64 :string :local-date])))
  (println)
  (println "| datatype | rows | impl | median ms | min ms | max ms |")
  (println "|---|---:|---|---:|---:|---:|")
  (doseq [datatype [:float64 :int64 :string :local-date]
          n [1000 100000 1000000]
          :let [ds (make-ds datatype n)
                opts (if (<= n 1000) {:warmup 200 :runs 50} {:warmup 10 :runs 15})]
          [impl-name f] impls]
    (let [row (try (let [{:keys [median min max]} (measure #(f ds 1) opts)]
                     (format "%.3f | %.3f | %.3f" median min max))
                   (catch Throwable e
                     (str "unsupported: " (.getSimpleName (class e)) " | | ")))]
      (println (format "| %s | %d | %s | %s |" (name datatype) n (name impl-name) row))
      (flush))))

(-main)
