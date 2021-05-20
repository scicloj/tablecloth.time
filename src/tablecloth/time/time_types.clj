(ns tablecloth.time.time-types
  (:require [tech.v3.datatype.casting :refer [add-object-datatype!]]))


(def additional-time-datatypes-to-klass-map
  {:year java.time.Year})


(defn additional-time-datatypes []
  (set (keys additional-time-datatypes-to-klass-map)))


(defn register-additional-time-datatypes! []
  (doseq [[dtype-kw time-klass] additional-time-datatypes-to-klass-map]
    (add-object-datatype! dtype-kw time-klass true)))
