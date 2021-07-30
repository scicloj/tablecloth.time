(ns tablecloth.time.api
  {:clj-kondo/config '{:linters {:unresolved-symbol {:level :off}}}}
  (:require [tech.v3.datatype.export-symbols :as exporter]
            [tablecloth.time.time-literals :refer [modify-printing-of-time-literals-if-enabled!]]))

(modify-printing-of-time-literals-if-enabled!)

(exporter/export-symbols tablecloth.time.api.slice
                         slice)

(exporter/export-symbols tablecloth.time.api.adjust-interval
                         adjust-interval)

(exporter/export-symbols tablecloth.time.api.index-by
                         index-by)

(exporter/export-symbols tablecloth.time.api.converters
                         convert-to
                         down-to-nearest
                         ->every
                         ->seconds
                         ->minutes
                         ->hours
                         ->days
                         ->weeks-end
                         ->months-end
                         ->quarters-end
                         ->years-end
                         ->every
                         string->time)
