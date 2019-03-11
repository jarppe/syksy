(ns syksy.web.cors
  (:require [sieppari.context :as sc])
  (:import (java.util.concurrent TimeUnit)))


(def preflight-response {:status  204
                         :headers {"access-control-allow-origin"      "*"
                                   "access-control-allow-headers"     "*"
                                   "access-control-allow-methods"     "GET, POST, PUT, PATCH, DELETE"
                                   "access-control-allow-credentials" "true"
                                   "access-control-max-age"           (->> (.toSeconds TimeUnit/DAYS 30) (str))
                                   "cache-control"                    (->> (.toSeconds TimeUnit/DAYS 30) (str "max-age="))}
                         :body    ""})


(defn cors-preflight-request? [request]
  (-> request :request-method (= :options)))


(defn update-with-cors-headers [request]
  (if-let [origin (-> request :headers (get "origin"))]
    (update preflight-response :headers assoc "access-control-allow-origin" origin)
    preflight-response))


(defn cors-interceptor []
  {:name  ::cors-interceptor
   :enter (fn [ctx]
            (let [request (-> ctx :request)]
              (if (cors-preflight-request? request)
                (->> request
                     (update-with-cors-headers)
                     (sc/terminate ctx))
                ctx)))})
