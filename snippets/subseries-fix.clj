;; Traces - add :type and :mode
{:x (vec (group-ds "Year"))
 :y (vec (group-ds "Cost"))
 :type "scatter"
 :mode "lines+markers"
 :xaxis (str "x" axis-suffix)
 :yaxis (str "y" axis-suffix)
 :name (str "Month " (inc idx))}

;; Layout - add :subplots to grid
{:grid
 {:rows 1
  :columns 12
  :pattern "independent"
  :subplots [["xy"  "xy2"  "xy3"  "xy4"
              "xy5" "xy6"  "xy7"  "xy8"
              "xy9" "xy10" "xy11" "xy12"]]}
 :showlegend false}
