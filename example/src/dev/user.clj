(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as igr :refer [clear reset-all]]
            [integrant.repl.state :as state]
            [syksy.config :as config]
            [app.components :as app]))

(igr/set-prep!
  (fn []
    (-> (config/load-config)
        (app/components)
        (doto (ig/load-namespaces)))))

(def reset igr/reset)
(def start igr/init)
(def stop igr/halt)
(defn system [] state/system)
