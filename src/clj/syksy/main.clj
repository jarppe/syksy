(ns syksy.main
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig])
  (:gen-class))

(defn start-app []
  (-> 'app.components/components
      (resolve)
      (apply nil)
      (ig/init)))

(defn -main [& args]
  (System/setProperty "org.jboss.logging.provider" "slf4j")
  (log/info "application starting...")
  (require 'app.components)
  (try
    (start-app)
    (log/info "application running!")
    (catch Throwable e
      (.println System/err "unexpected error while starting app")
      (.printStackTrace e System/err)
      (System/exit 1))))
