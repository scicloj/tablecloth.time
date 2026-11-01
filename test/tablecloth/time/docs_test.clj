(ns tablecloth.time.docs-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]))

(def chapter-02-page "docs/chapter_02_plotje.html")

(def chapter-02-figure-count 17)

(def chapter-02-size-budget
  "Byte budget for the rendered Chapter 2 page: about 20% above the 8.2 MB
  it measured when Clay 2.0.24 rendered its 17 Plotje SVGs (it was 12.6 MB
  when Clay 2.0.11 printed them as Hiccup)."
  10000000)

(defn count-matches [re s]
  (count (re-seq re s)))

(deftest chapter-02-page-test
  (let [f    (io/file chapter-02-page)
        html (slurp f)]
    (testing "every Plotje figure renders as an SVG element"
      (is (= chapter-02-figure-count
             (count-matches #"<svg[^>]*class=\"plotje-plot\"" html))))
    (testing "no figure is printed as Clojure Hiccup"
      (is (zero? (count-matches #"\[:polyline" html)))
      (is (zero? (count-matches #"\[:svg" html))))
    (testing "the page stays within its size budget"
      (is (<= (.length f) chapter-02-size-budget)))))
