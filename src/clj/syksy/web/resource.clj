(ns syksy.web.resource
  (:require [integrant.core :as ig]
            [clojure.tools.logging :as log]
            [syksy.web.resources.core :as res])
  (:import (java.util.regex Pattern)))

(defn- re? [v]
  (instance? Pattern v))

(defmethod ig/init-key ::handler [_ {:keys [match? resource-name] :as opts}]
  (assert (or (and (string? match?)
                   (string? resource-name))
              (and (re? match?)
                   (string? resource-name))
              (ifn? match?)))
  (assert (or (nil? resource-name)
              (string? resource-name))
          "`resource-name` must be a string")
  (let [resource-name-fn (cond
                           (string? match?) (fn [uri]
                                              (when (= uri match?)
                                                resource-name))
                           (re? match?) (fn [uri]
                                          (when (re-matches match? uri)
                                            resource-name))
                           (ifn? match?) match?)]
    (log/infof "rewrite handler: match?=%s, resource-name=%s"
               (pr-str match?)
               (pr-str resource-name))
    (-> (assoc opts :resource-name-fn resource-name-fn)
        (res/make-resource-handler))))
