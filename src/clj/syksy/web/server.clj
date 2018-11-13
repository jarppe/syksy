(ns syksy.web.server
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [immutant.web :as immutant]))


(def defaults {:host "localhost"
               :port 4000
               :path "/"})


(defmethod ig/init-key :syksy.web/server [_ {:keys [router] :as opts}]
  (assert (ifn? router) "router must be a fn")
  (let [opts (->> opts
                  (filter (comp some? val))
                  (into {})
                  (merge defaults))]
    (log/infof "making web server: host=%s, port=%d, path=%s"
               (:host opts)
               (:port opts)
               (:path opts))
    (immutant/run router (dissoc opts :router))))


(defmethod ig/halt-key! :syksy.web/server [_ server]
  (log/info "stopping web server")
  (immutant/stop server))
