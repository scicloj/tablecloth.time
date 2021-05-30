(ns tablecloth.time.protocols.parseable)

(defprotocol Parseable
  (parse [str] "Parse string to datetime."))
