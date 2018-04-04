(ns syksy.web.api
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.middleware.params :as params]
            [muuntaja.core :as muuntaja]
            [muuntaja.middleware :as middleware]
            [muuntaja.format.jsonista :as json-format]
            [syksy.web.cache :as cache]))

(defn wrap-ctx [handler ctx]
  (fn [request]
    (-> request
        (assoc :ctx ctx)
        (handler))))

(defn apply-middleware [handler middleware]
  (reduce (fn [handler middleware]
            (middleware handler))
          handler
          middleware))

(defmethod ig/init-key ::handler [_ {:keys [routes ctx middleware]}]
  (assert (fn? routes) "routes is mandatory and needs to be fn")
  (log/info "creating api-handler")
  (-> routes
      (wrap-ctx ctx)
      (middleware/wrap-format (-> muuntaja/default-options
                                  json-format/with-json-format
                                  muuntaja/create))
      (params/wrap-params)
      (cache/wrap-no-store)
      (apply-middleware middleware)))
