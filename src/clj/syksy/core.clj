(ns syksy.core
  (:require [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [syksy.web.index :as index]))

(defn default-components [{:keys [index-body routes ctx middleware]}]
  {:syksy.web.server/server {:handlers [(ig/ref :syksy.web.api/handler)
                                        (ig/ref :syksy.web.index/handler)
                                        (ig/ref :syksy.web.resources/handler)
                                        (ig/ref :syksy.web.not-found/handler)]}
   :syksy.web.resources/handler {}
   :syksy.web.not-found/handler {}
   :syksy.web.index/handler {:index-body (or index-body (index/index {}))}
   :syksy.web.api/handler {:routes (or routes
                                       (-> {:message "Syksy is ready"}
                                           (resp/ok)
                                           (constantly)))
                           :ctx ctx
                           :middleware middleware}})
