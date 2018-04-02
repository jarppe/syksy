(ns syksy.web.resources
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [ring.util.mime-type :refer [default-mime-types]]
            [syksy.web.cache :as cache]
            [syksy.util.checksum :as checksum])
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

(defmethod ig/init-key ::handler [_ {:keys [asset-prefix asset-dir mime-types checksum-fn]
                                     :or {asset-prefix "/asset/"
                                          asset-dir "public/"
                                          checksum-fn default-checksum}}]
  (assert (fn? checksum-fn) "checksum-fn must be a fn")
  (let [; Ensure asset-prefix starts and ends with "/"
        asset-prefix (str (if-not (str/starts-with? asset-prefix "/") "/")
                          asset-prefix
                          (if-not (str/ends-with? asset-prefix "/") "/"))
        ; Ensure asset-dir ends with "/"
        asset-dir (str asset-dir
                       (if-not (str/ends-with? asset-dir "/") "/"))
        asset-prefix-len (count asset-prefix)
        resource-name->mime-type (make-resource-name->mime-type mime-types)
        not-modified (resp/not-modified)]
    (log/infof "resource handler: asset-prefix=%s, asset-dir=%s" (pr-str asset-prefix) (pr-str asset-dir))
    (fn [request]
      (when (and (-> request :request-method (= :get))
                 (-> request :uri (str/starts-with? asset-prefix)))
        (let [resource-name (str asset-dir (-> request :uri (subs asset-prefix-len)))]
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
