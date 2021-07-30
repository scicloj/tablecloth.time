(ns tablecloth.time.validatable)

(defn add-validatable
  "Add metadata derived from a few columns,
  validatable by checking if these columns
  are identical to their values when it was created."
  [ds column-names name metadata]
  (-> ds
      (vary-meta assoc-in
                 [:validatable name]
                 {:column-names column-names
                  :columns      (select-keys ds column-names)
                  :metadata         metadata})))

(defn valid?
  "Check if metadata derived from a few columns is still valid
  (that is, if the columns have not changed)."
  [ds name]
  (if-let [{:keys [columns column-names]} (-> ds
                                           meta
                                           :validatable
                                           (get name))]
    (->> column-names
         (every? (fn [column-name]
                   (identical? (ds column-name)
                               (columns column-name)))))
    false))

