(ns plotje-review
  "Export the Plotje notebook's figures for review and render it with Clay.
   Run: `clj -M:notebooks -m plotje-review`"
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [scicloj.clay.v2.api :as clay]
            [scicloj.plotje.api :as pj]
            [chapter-02-plotje :as ch2]))

(def out-dir "target/plotje-review")

(defn export-figure!
  [[fig-name pose]]
  (let [path #(str out-dir "/" fig-name %)]
    (pj/save pose (path ".png"))
    (pj/save pose (path ".svg"))
    (spit (path ".summary.edn")
          (with-out-str (pprint/pprint (pj/svg-summary pose))))
    (path ".png")))

(defn -main [& _]
  (.mkdirs (io/file out-dir))
  (run! (comp println export-figure!) ch2/figures)
  (clay/make! {:source-path "notebooks/chapter_02_plotje.clj"
               :show false})
  (shutdown-agents)
  (System/exit 0))
