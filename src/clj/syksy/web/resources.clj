(ns syksy.web.resources
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.http-response :as resp]
            [ring.util.mime-type :refer [default-mime-types]]
            [syksy.util.checksum :as checksum]
            [syksy.web.cache :as cache])
  (:import (org.apache.commons.io FilenameUtils)))

;;
;; MIME support:
;;

(def mime-type
  (let [mime-types (->> (assoc default-mime-types "js" "application/javascript; charset=utf-8")
                        (map (fn [[k v]]
                               (if (str/starts-with? v "text/")
                                 [k (str v "; charset=utf-8")]
                                 [k v])))
                        (into {}))]
    (fn [filename]
      (get mime-types (FilenameUtils/getExtension ^String filename) "application/octet-stream"))))

;;
;; ETag support:
;;

(defn etag [resource-name]
  (with-open [in (-> resource-name
                     io/resource
                     io/input-stream)]
    (-> in
        (checksum/hash-input-stream)
        (str))))

;;
;; Development resource locator:
;;

(defn make-dev-resource-locator []
  (fn [resource-name]
    (when (io/resource resource-name)
      {:resource  resource-name
       :etag      (etag resource-name)
       :mime-type (mime-type resource-name)})))

;;
;; Make resource handler:
;;

(defn make-resource-handler [resource-locator]
  (fn [request]
    (let [resource-name (-> request
                            :reitit.core/match
                            :path-params
                            :resource)
          resource-info (-> (str "public/" resource-name)
                            (resource-locator))
          if-none-match (-> request
                            :headers
                            (get cache/if-none-match))
          etag          (-> resource-info
                            :etag)
          mime-type     (-> resource-info
                            :mime-type)
          not-found     (resp/not-found "can't locate resource")]
      (if resource-info
        (if (and (some? if-none-match)
                 (some? etag)
                 (= etag if-none-match))
          (resp/not-modified)
          (-> resource-info
              (get :resource)
              (io/resource)
              (io/input-stream)
              (resp/ok)
              (assoc :headers {"content-type"      mime-type
                               "etag"              etag
                               cache/cache-control cache/cache-control-no-cache})))
        not-found))))

;;
;; Resource handler component:
;;

(defmethod ig/init-key ::handler [_ _]
  ; TODO: make prod-resource-locator
  (make-resource-handler (make-dev-resource-locator)))
