;; Minimal subplot test
;; Run this to verify Plotly subplots work at all

;; Copy this into a Clay notebook and eval:

(kind/plotly
 {:data [{:x [1 2 3]
          :y [4 5 6]
          :type "scatter"
          :mode "lines+markers"
          :name "Left"}
         {:x [1 2 3]
          :y [6 5 4]
          :type "scatter"
          :mode "lines+markers"
          :xaxis "x2"
          :yaxis "y2"
          :name "Right"}]
  :layout {:xaxis {:domain [0 0.45]
                   :title "X1"}
           :xaxis2 {:domain [0.55 1]
                    :title "X2"}
           :yaxis {:title "Y1"}
           :yaxis2 {:anchor "x2"
                    :title "Y2"}
           :title "Two subplots test"}})
