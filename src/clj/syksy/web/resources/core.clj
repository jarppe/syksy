(ns syksy.web.resources.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.http-response :as resp]
            [ring.util.mime-type :refer [default-mime-types]]
            [syksy.util.mode :as mode]
            [syksy.util.checksum :as checksum]
            [syksy.web.cache :as cache]
            [syksy.web.resources.caching-checksum :as caching-checksum])
  (:import (org.apache.commons.io FilenameUtils)))

(defn- make-resource-name->mime-type [mime-type-overrides]
  (let [with-charset-utf8 (fn [mime-types]
                            (->> mime-types
                                 (map (fn [[k v]]
                                        (if (str/starts-with? v "text/")
                                          [k (str v "; charset=utf-8")]
                                          [k v])))
                                 (into {})))
        mime-types (-> default-mime-types
                       (assoc "js" "application/javascript; charset=utf-8")
                       (with-charset-utf8)
                       (merge mime-type-overrides))]
    (fn [^String filename]
      (get mime-types (FilenameUtils/getExtension filename) "application/octet-stream"))))

(defn default-checksum [resource-name]
  (when-let [resource (-> resource-name io/resource)]
    (with-open [in (-> resource io/input-stream)]
      (-> in checksum/hash-input-stream str))))

(defn make-resource-handler [{:keys [mime-types checksum-fn resource-name-fn]}]
  (assert (or (nil? checksum-fn) (ifn? checksum-fn)) "checksum-fn must be a fn")
  (assert (ifn? resource-name-fn) "`resource-name-fn` must be a function")
  (let [resource-name->mime-type (make-resource-name->mime-type mime-types)
        checksum-fn (or checksum-fn
                        (if (mode/dev-mode?)
                          default-checksum
                          caching-checksum/caching-checksum))
        not-modified (resp/not-modified)]
    (fn [request]
      (when (-> request :request-method (= :get))
        (when-let [resource-name (-> request :uri resource-name-fn)]
          (when-let [resource-checksum (checksum-fn resource-name)]
            (if (-> request :headers (get cache/if-modified-since) (= resource-checksum))
              not-modified
              (-> resource-name
                  (io/resource)
                  (io/input-stream)
                  (resp/ok)
                  (resp/content-type (-> resource-name resource-name->mime-type))
                  (resp/header "etag" resource-checksum)
                  (resp/header cache/cache-control cache/cache-control-no-cache)))))))))
