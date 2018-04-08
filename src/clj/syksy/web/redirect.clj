(ns syksy.web.redirect
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [clojure.tools.logging :as log])
  (:import (java.util.regex Pattern)))

(defn- re? [v]
  (instance? Pattern v))

(def http-redirect #{301 302 303 307 308})

(defmethod ig/init-key ::handler [_ {:keys [from location status body]
                                     :or {status 307}}]
  (assert (or (string? from)
              (fn? from)
              (re? from))
          "`from` must be a string, function or regular expression")
  (assert (string? location)
          "`location` must be a string")
  (assert (http-redirect status)
          (str "`status` must be one of " (->> http-redirect sort (str/join ", "))))
  (log/infof "redirect: from=%s, location=%s, status=%d" (pr-str from) (pr-str location) status)
  (let [match? (cond
                 (string? from) (comp (partial = from) :uri)
                 (fn? from) from
                 (re? from) (comp (partial re-matches from) :uri))
        redirect {:status status
                  :headers {"location" location}
                  :body (or body "")}]
    (fn [request]
      (when (match? request)
        redirect))))
