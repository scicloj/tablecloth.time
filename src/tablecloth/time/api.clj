(ns tablecloth.time.api
  ;; {:clj-kondo/config '{:linters {:unresolved-symbol {:level :off}}}}
  (:require [tech.v3.datatype.export-symbols :as exporter]))

(exporter/export-symbols tablecloth.time.api.slice
                         slice)
