(ns tablecloth.time.api.rolling)

;; Placeholder namespace for future rolling-window API.
;; Currently just defines a stub so the namespace loads cleanly.

(defn rolling
  "Rolling window API (work in progress). Not yet implemented."
  ([ds time-col window-spec columns-map]
   (throw (ex-info "tablecloth.time.api/rolling is not implemented yet"
                   {:type ::not-implemented
                    :time-col time-col
                    :window-spec window-spec
                    :columns-map (keys columns-map)}))))
