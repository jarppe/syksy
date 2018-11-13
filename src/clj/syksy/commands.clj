(ns syksy.commands
  (:require [integrant.core :as ig]))

(defmethod ig/init-key ::commands [_ {:keys [commands]}]
  ;; TODO: validate commands
  commands)
