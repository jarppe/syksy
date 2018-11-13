(ns syksy.web.server-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]
            [integrant.core :as ig]
            [clj-http.client :as http]
            [ring.util.http-response :as resp]
            [muuntaja.core :as muuntaja]
            [potpuri.core :as p]
            [syksy.core :as core]))

(def ^:dynamic system nil)
(def ^:dynamic http-port nil)

(defn with-web-server [test-config components]
  (fn [f]
    (let [http-port' (with-open [s (java.net.ServerSocket. 0)]
                       (.getLocalPort s))
          config     (p/deep-merge
                       test-config
                       {:http {:port http-port'}})
          system'    (-> (core/default-components config)
                         (merge components)
                         (doto (ig/load-namespaces))
                         (ig/init))]
      (try
        (binding [http-port http-port'
                  system    system']
          (f))
        (finally
          (ig/halt! system'))))))

(comment
  (do (require 'syksy.web.server)
      (require 'syksy.web.router)
      (require 'syksy.web.resources)
      (let [config     nil
            components nil
            system     (-> (core/default-components config)
                           ;(merge components)
                           ;(doto (ig/load-namespaces))
                           (ig/init))]
        (try
          (println system)
          (finally
            (ig/halt! system))))))

;;
;; HTTP:
;;

(defn ->url [uri]
  (str "http://localhost:" (or http-port 4000) uri))


(defn ->headers [headers]
  (merge {"Accept"       "application/edn"
          "Content-Type" "application/edn"}
         headers))


(def +muuntaja+ (muuntaja/create))


(defn content-type [headers]
  (some-> headers
          (get "Content-Type")
          (str/split #"\s*;\s*")
          (first)))


(defn decode-body [response]
  (let [format  (-> response :headers content-type)
        decoder (or (muuntaja/decoder +muuntaja+ format)
                    (fn [body charset] (slurp body)))]
    (assoc response :body (some-> response
                                  :body
                                  (decoder "utf-8")))))


(defn encode-body [body headers]
  (let [format (-> headers content-type)]
    (muuntaja/encode +muuntaja+ format body)))

;;
;; HTTP GET and POST:
;;

(defn GET
  ([uri] (GET uri {}))
  ([uri headers]
   (-> (http/get (->url uri)
                 {:headers           (->headers headers)
                  :redirect-strategy :none
                  :as                :stream
                  :throw-exceptions  false})
       decode-body)))


(defn POST
  ([uri] (POST uri nil nil))
  ([uri body] (POST uri body nil))
  ([uri body headers]
   (let [headers (->headers headers)
         body    (encode-body body headers)]
     (-> (http/post (->url uri)
                    {:body              body
                     :headers           headers
                     :redirect-strategy :none
                     :as                :stream
                     :throw-exceptions  false})
         decode-body))))

;;
;; Test fixture:
;;

(use-fixtures :once (with-web-server nil nil))

;;
;; Tests:
;;

(deftest redirect-is-observed
  (fact
    (GET "/")
    => {:status  307
        :headers {"location" "/web/"}})
  (fact
    (GET "/web")
    => {:status  307
        :headers {"location" "/web/"}}))


(deftest index-page-is-served
  (fact
    (GET "/web/")
    => {:status  200
        :headers {"Content-Type"  "text/html; charset=utf-8"
                  "cache-control" "private, no-cache"
                  "etag"          (re-pattern #"\d+")}
        :body    #(-> % (str/index-of "<title>Another fancy Syksy app</title>") (pos?))})
  (let [etag (-> (GET "/web/")
                 :headers
                 (get "etag"))]
    (fact
      (GET "/web/" {"if-none-match" etag})
      => {:status 304})
    (fact
      (GET "/web/" {"if-none-match" "foo"})
      => {:status 200})))


(deftest not-found-is-served
  (fact
    (GET "/foozaa")
    => {:status 404}))


(deftest resources-are-served
  (fact
    (GET "/asset/foo.css")
    => {:status  200
        :headers {"Content-Type"  "text/css; charset=utf-8"
                  "cache-control" "private, no-cache"
                  "etag"          (re-pattern #"\d+")}})

  (fact
    (GET "/asset/foo.js")
    => {:status  200
        :headers {"Content-Type" "application/javascript; charset=utf-8"}})

  (fact
    (GET "/asset/foo.txt")
    => {:status  200
        :headers {"Content-Type" "text/plain; charset=utf-8"}
        :body    "Hello, world!"})

  (fact
    (GET "/asset/foo.bar")
    => {:status 404
        :body   "can't locate resource"})

  (let [etag (-> (GET "/asset/foo.txt")
                 :headers
                 (get "etag"))]
    (fact
      (GET "/asset/foo.txt" {"if-none-match" etag})
      => {:status 304})
    (fact
      (GET "/asset/foo.txt" {"if-none-match" "foo"})
      => {:status  200
          :headers {"etag" etag}})))
