(ns syksy.web.resources.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.http-response :as resp]
            [ring.util.mime-type :refer [default-mime-types]]
            [syksy.util.mode :as mode]
            [syksy.util.checksum :as checksum]
            [syksy.web.cache :as cache]
            [syksy.web.resources.caching-checksum :as caching-checksum])
  (:import (org.apache.commons.io FilenameUtils)
           (java.io InputStream PipedInputStream PipedOutputStream)
           (java.util.zip GZIPOutputStream)))

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

;;
;; gzip support:
;;

(defn accept-gzip? [request]
  (some-> request
          :headers
          (get "accept-encoding")
          (str/includes? "gzip")))


(defn gzip-content ^InputStream [^InputStream in gzip?]
  (if gzip?
    ; TODO: There should be a better way to compress input-stream?!?!
    (let [zipped (PipedInputStream.)]
      (future
        (with-open [zipper (-> zipped
                               PipedOutputStream.
                               GZIPOutputStream.)]
          (io/copy in zipper)))
      zipped)
    in))

;;
;; Make resource handler:
;;

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
            (if (-> request :headers (get cache/if-none-match) (= resource-checksum))
              not-modified
              (let [gzip? (accept-gzip? request)]
                (-> resource-name
                    (io/resource)
                    (io/input-stream)
                    (gzip-content gzip?)
                    (resp/ok)
                    (update :headers (fn [headers]
                                       (-> headers
                                           (assoc "content-type" (-> resource-name resource-name->mime-type))
                                           (assoc "etag" resource-checksum)
                                           (assoc cache/cache-control cache/cache-control-no-cache)
                                           (assoc "content-encoding" (if gzip? "gzip" "identity"))))))))))))))
