(ns syksy.web.resources
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [ring.util.mime-type :refer [default-mime-types]]
            [syksy.web.resources.core :as res]))

(defmethod ig/init-key ::handler [_ {:keys [asset-prefix asset-dir]
                                     :or {asset-prefix "/asset/"
                                          asset-dir "public/"}
                                     :as opts}]
  (let [; Ensure asset-prefix starts and ends with "/"
        asset-prefix (str (if-not (str/starts-with? asset-prefix "/") "/")
                          asset-prefix
                          (if-not (str/ends-with? asset-prefix "/") "/"))
        ; Ensure asset-dir ends with "/"
        asset-dir (str asset-dir
                       (if-not (str/ends-with? asset-dir "/") "/"))
        asset-prefix-len (count asset-prefix)
        resource-name-fn (fn [uri]
                           (when (str/starts-with? uri asset-prefix)
                             (str asset-dir (subs uri asset-prefix-len))))]
    (log/infof "resource handler: asset-prefix=%s, asset-dir=%s"
               (pr-str asset-prefix)
               (pr-str asset-dir))
    (-> (assoc opts :resource-name-fn resource-name-fn)
        (res/make-resource-handler))))
