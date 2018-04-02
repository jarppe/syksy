(ns syksy.web.not-found
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]))

(defmethod ig/init-key ::handler [_ {:keys [body] :or {body "Not found"}}]
  (log/info "making not-found handler")
  (constantly (resp/not-found body)))
