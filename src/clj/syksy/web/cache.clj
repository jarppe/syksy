(ns syksy.web.cache
  (:require [clojure.string :as str]))

(def cache-control "cache-control")
(def cache-control-30d "public, max-age=2592000, s-maxage=2592000")
(def cache-control-no-cache "no-cache")
(def cache-control-no-store "no-store, must-revalidate")

(def vary "vary")
(def accept-encoding "accept-encoding")

(def if-none-match "if-none-match")

(defn wrap-cache [handler cache-control-value]
  (fn [req]
    (let [response (handler req)]
      (if (and (-> response map?)
               (-> response :headers (get cache-control) nil?))
        (update response :headers
                assoc cache-control cache-control-value
                      vary accept-encoding)
        response))))

(def wrap-30d-cache (fn [handler] (wrap-cache handler cache-control-30d)))
(def wrap-no-cache (fn [handler] (wrap-cache handler cache-control-no-cache)))
(def wrap-no-store (fn [handler] (wrap-cache handler cache-control-no-store)))

(defn wrap-cache-resource [handler]
  (fn [req]
    (let [response (handler req)]
      (if (and (-> response map?)
               (-> response :headers (get cache-control) nil?))
        (update response :headers
                assoc cache-control
                      (if (some-> req :query-string (str/starts-with? "v="))
                        cache-control-30d
                        cache-control-no-cache)
                      vary accept-encoding)
        response))))
