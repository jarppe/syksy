(ns syksy.main
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [syksy.util.mode :as mode])
  (:gen-class))

(defn start-app []
  (-> 'app.components/components
      (resolve)
      (apply nil)
      (doto (ig/load-namespaces))
      (ig/init)))

(defn -main [& args]
  (System/setProperty "org.jboss.logging.provider" "slf4j")
  (log/info (format "application starting, mode=%s..." (-> (mode/mode) name str/upper-case)))
  (require 'app.components)
  (try
    (start-app)
    (log/info "application running!")
    (catch Throwable e
      (.println System/err "unexpected error while starting app")
      (.printStackTrace e System/err)
      (System/exit 1))))
