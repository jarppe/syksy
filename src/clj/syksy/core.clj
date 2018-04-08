(ns syksy.core
  (:require [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [syksy.web.index :as index]))

(defn default-components [{:keys [host port index-body routes ctx middleware handlers addon-handlers]
                           :or {host "localhost"
                                port 3000}}]
  {[:syksy.web.server/server ::syksy] {:host host
                                       :port port
                                       :handlers (or handlers
                                                    (concat [(ig/ref [:syksy.web.api/handler ::syksy])
                                                             (ig/ref [:syksy.web.index/handler ::syksy])
                                                             (ig/ref [:syksy.web.resources/handler ::syksy])]
                                                            addon-handlers
                                                            [(ig/ref [:syksy.web.not-found/handler ::syksy])]))}
   [:syksy.web.resources/handler ::syksy] {}
   [:syksy.web.not-found/handler ::syksy] {}
   [:syksy.web.index/handler ::syksy] {:index-body (or index-body (index/index {}))}
   [:syksy.web.api/handler ::syksy] {:routes (or routes
                                                (-> {:message "Syksy is ready"}
                                                    (resp/ok)
                                                    (constantly)))
                                    :ctx ctx
                                    :middleware middleware}})
