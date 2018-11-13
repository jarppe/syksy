(ns syksy.config
  (:require [clojure.walk :as walk]
            [integrant.core :as ig]
            [maailma.core :as m]))

(defn coerce [config]
  (walk/prewalk (fn [v]
                  (if (and (string? v)
                           (re-matches #"\d+" v))
                    (Long/parseLong v)
                    v))
                config))

(defn load-config []
  (let [heroku-port (some-> (System/getenv "PORT")
                            (Long/parseLong))
        config      (m/build-config
                      (m/resource "config.edn" {:readers {'ig/ref ig/ref}})
                      (m/file "./config.edn" {:readers {'ig/ref ig/ref}})
                      (coerce (m/properties "app"))
                      (coerce (m/env "app")))]
    (if (and (-> config :http :port nil?)
             heroku-port)
      (update config :http assoc :port heroku-port)
      config)))
