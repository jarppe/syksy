(ns app.components
  (:require [syksy.core :as syksy]
            [ring.util.http-response :as resp]
            [schema.core :as s]))

(def commands
  [{:name     :health/ping
    :method   :get
    :response {:message s/Str}
    :handler  (fn [request]
                (resp/ok {:message "pong"}))}])


(defn components [config]
  (-> config
      (assoc :commands commands)
      (syksy/default-components)))
