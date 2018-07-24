(ns syksy.web.server
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [immutant.web :as immutant]))

(def defaults {:host "localhost"
               :port 3000
               :path "/"})

(defmethod ig/init-key ::server [_ {:keys [handlers middleware] :as opts}]
  (assert (sequential? handlers) "handlers is mandatory seq of handlers")
  (assert (every? ifn? handlers) "each handler must be a fn")
  (let [opts (-> defaults
                 (merge opts)
                 (dissoc :handlers)
                 (dissoc :middleware))
        handler (reduce (fn [handler mw]
                          (cond
                            (vector? mw) (apply (first mw) handler (rest mw))
                            (ifn? mw) (mw handler)
                            :else (throw (ex-info (str "don't understand middeware: " (pr-str mw)) {}))))
                        (apply some-fn handlers)
                        (reverse middleware))]
    (log/infof "making web server: host=%s, port=%d, path=%s"
               (:host opts)
               (:port opts)
               (:path opts))
    (immutant/run handler opts)))

(defmethod ig/halt-key! ::server [_ server]
  (log/info "stopping web server")
  (immutant/stop server))
