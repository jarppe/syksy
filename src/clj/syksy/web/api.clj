(ns syksy.web.api
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.middleware.params :as params]
            [muuntaja.middleware :as muuntaja]
            [syksy.web.cache :as cache]))

(defmethod ig/init-key ::handler [_ {:keys [routes]}]
  (assert (fn? routes) "routes is mandatory and needs to be fn")
  (-> routes
      (muuntaja/wrap-format)
      (params/wrap-params)
      (cache/wrap-no-store)))
