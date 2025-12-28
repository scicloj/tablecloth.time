(ns tablecloth.time.api
  (:require [tablecloth.time.time-literals :refer [modify-printing-of-time-literals-if-enabled!]]))

(modify-printing-of-time-literals-if-enabled!)

;; NOTE: The following legacy APIs have been removed and their tests archived:
;; - tablecloth.time.api.slice (slice, index-by - to be reimplemented)
;; - tablecloth.time.api.adjust-frequency (adjust-frequency - to be reimplemented)
;; - tablecloth.time.api.rolling-window (rolling-window - to be reimplemented)
;; - tablecloth.time.api.converters (various converters - functionality moved to column.api)
;; - tablecloth.time.api.time-components (field extractors - functionality moved to column.api)
;; See test/_archive/README.md for archived tests and development-plan.md for the new architecture.
