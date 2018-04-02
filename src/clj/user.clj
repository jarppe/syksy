(ns user
  (:require [integrant.repl :as igr :refer [clear reset-all]]
            [integrant.repl.state :as state]))

(igr/set-prep!
  (fn []
    (require 'app.components)
    (-> 'app.components/components resolve (apply nil))))

(def reset igr/reset)
(def start igr/init)
(def stop igr/halt)
(def system state/system)
