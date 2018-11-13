(ns syksy.core
  (:require [integrant.core :as ig]
            [syksy.web.index :as index]))

(defn default-components [config]
  {:syksy.web/server            {:host   (-> config :http :host)
                                 :port   (-> config :http :port)
                                 :router (ig/ref :syksy.web/router)}

   :syksy.web/router            {:index-body       (or (-> config :index-body)
                                                       (-> config :index (index/index)))
                                 :interceptors     (-> config :interceptors)
                                 :resource-handler (ig/ref :syksy.web.resources/handler)
                                 :commands         (ig/ref :syksy.commands/commands)}

   :syksy.web.resources/handler {}

   :syksy.commands/commands     {:commands (-> config :commands)}})
