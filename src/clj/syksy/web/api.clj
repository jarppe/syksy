(ns syksy.web.api
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.middleware.params :as params]
            [muuntaja.middleware :as muuntaja]
            [syksy.web.cache :as cache]))

(defn wrap-ctx [handler ctx]
  (fn [request]
    (-> request
        (assoc :ctx ctx)
        (handler))))

(defmethod ig/init-key ::handler [_ {:keys [routes ctx]}]
  (assert (fn? routes) "routes is mandatory and needs to be fn")
  (-> routes
      (wrap-ctx ctx)
      (muuntaja/wrap-format)
      (params/wrap-params)
      (cache/wrap-no-store)))
