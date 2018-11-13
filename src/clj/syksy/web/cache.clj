(ns syksy.web.cache
  (:require [clojure.string :as str]
            [ring.util.http-response :as resp])
  (:import (java.util.concurrent TimeUnit)))

(def ^:const cache-control "cache-control")
(def ^:const cache-control-no-cache "private, no-cache")
(def ^:const cache-control-no-store "private, no-cache, no-store")
(def ^:const cache-control-cache-30d (str "max-age=" (.toSeconds TimeUnit/DAYS 30)))

(def ^:const vary "vary")
(def ^:const accept-encoding "accept-encoding")

(def ^:const if-none-match "if-none-match")

(defn cache-interceptor []
  {:leave (fn [ctx]
            (if (-> ctx :response :headers (contains? cache-control))
              ctx
              (update-in ctx [:response :headers] assoc
                         cache-control cache-control-no-store
                         vary accept-encoding)))})

(defn with-cache-control [response cache-control-value]
  (update response :headers assoc cache-control cache-control-value))

(defn get-if-none-match [request]
  (-> request :headers (get if-none-match)))

(defn with-etag [response etag-value]
  (update response :headers assoc "etag" etag-value))

(defn not-modified-if-etag-match
  "Accepts a request and a expected etag value. The `request` is expected
  to be a HTTP request and the `etag-value` is the resources etag value.
  If the request has an \"If-None-Match\" header with expected value, this
  function returns HTTP 304 Not Modified response, otherwise a `nil` is
  returned."
  [request etag-value]
  (when (some-> request (get-if-none-match) (= etag-value))
    (resp/not-modified)))
