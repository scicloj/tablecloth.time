(ns tablecloth.time.derived)

(defn add-derived
  "Add metadata derived from a few columns."
  [ds column-names id data]
  (-> ds
      (vary-meta assoc-in
                 [:derived id]
                 {:column-names column-names
                  :columns      (select-keys ds column-names)
                  :data         data})))

(defn valid?
  "Check if metadata derived from a few columns is still valid
(that is, if the columns have not changed)."
  [ds id]
  (let [{:keys [columns column-names]} (-> ds
                                           meta
                                           :derived
                                           (get id))]
    (->> column-names
         (every? (fn [column-name]
                   (identical? (ds column-name)
                               (columns column-name)))))))

