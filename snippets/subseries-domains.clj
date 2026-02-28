;; Build layout with explicit axis domains
;; Each subplot gets 1/12 of the width

(defn make-axis-domain [idx total]
  (let [width (/ 1.0 total)
        gap 0.02
        start (+ (* idx width) (if (zero? idx) 0 gap))
        end (* (inc idx) width)]
    [start end]))

;; Generate layout with 12 x-axes
(def subseries-layout
  (let [n 12]
    (reduce
      (fn [layout idx]
        (let [axis-key (if (zero? idx) :xaxis (keyword (str "xaxis" (inc idx))))
              y-axis-key (if (zero? idx) :yaxis (keyword (str "yaxis" (inc idx))))
              domain (make-axis-domain idx n)]
          (-> layout
              (assoc axis-key {:domain domain :anchor (if (zero? idx) "y" (str "y" (inc idx)))})
              (assoc y-axis-key {:anchor (if (zero? idx) "x" (str "x" (inc idx)))}))))
      {:showlegend false
       :title "Subseries: A10 Drug Sales by Month"}
      (range n))))

;; Full example
(kind/plotly
  {:data a10-subseries
   :layout subseries-layout})
