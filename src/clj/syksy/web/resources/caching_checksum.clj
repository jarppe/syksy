(ns syksy.web.resources.caching-checksum
  (:require [clojure.java.io :as io]
            [syksy.util.checksum :as checksum]))

(defonce checksum-cache (atom {}))

(defn caching-checksum [resource-name]
  (if-let [checksum (get @caching-checksum resource-name)]
    checksum
    (when-let [resource (-> resource-name io/resource)]
      (let [checksum (with-open [in (-> resource io/input-stream)]
                       (-> in checksum/hash-input-stream str))]
        (swap! checksum-cache assoc resource-name checksum)
        checksum))))
