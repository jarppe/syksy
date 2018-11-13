(ns syksy.main
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [syksy.config :as config]
            [syksy.util.mode :as mode])
  (:import (org.slf4j.bridge SLF4JBridgeHandler))
  (:gen-class))


(defn start-app []
  (require 'app.components)
  (-> 'app.components/components
      (resolve)
      (apply [(config/load-config)])
      (doto (ig/load-namespaces))
      (ig/init)))


(defn -main [& _]
  (SLF4JBridgeHandler/install)
  (System/setProperty "org.jboss.logging.provider" "slf4j")
  (log/info (format "application starting, mode=%s..." (-> (mode/mode) name str/upper-case)))
  (try
    (start-app)
    (log/info "application running!")
    (catch Throwable e
      (.println System/err "unexpected error while starting app")
      (.printStackTrace e System/err)
      (System/exit 1))))