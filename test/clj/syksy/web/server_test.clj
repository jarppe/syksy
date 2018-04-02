(ns syksy.web.server-test
  (:require [clojure.test :refer :all]
            [testit.core :refer :all]
            [integrant.core :as ig]
            [clj-http.client :as http]
            [ring.util.http-response :as resp]
            [syksy.web.index :as index]))

(def ^:dynamic system nil)

(defn with-web-server [f]
  (let [config {:syksy.web.server/server {:port 3001
                                          :handlers [(ig/ref :syksy.web.api/handler)
                                                     (ig/ref :syksy.web.index/handler)
                                                     (ig/ref :syksy.web.resources/handler)
                                                     (ig/ref :syksy.web.not-found/handler)]}
                :syksy.web.api/handler {:routes (fn [request]
                                                  (when (-> request :uri (= "/api"))
                                                    (resp/ok {:hello "world"})))}
                :syksy.web.index/handler {:index-body (index/index {:title "Syksy test"})}
                :syksy.web.resources/handler {:asset-dir "server-test-resources"}
                :syksy.web.not-found/handler {}}]
    (ig/load-namespaces config)
    (let [s (ig/init config)]
      (try
        (binding [system s]
          (f))
        (finally
          (ig/halt! s))))))

(use-fixtures :once with-web-server)

(deftest api-is-served
  (fact
    (http/get "http://localhost:3001/api" {:throw-exceptions false
                                           :accept :json})
    =in=> {:status 200
           :headers {"Content-Type" "application/json; charset=utf-8"
                     "cache-control" "no-store, must-revalidate"
                     "vary" "accept-encoding"}
           :body "{\"hello\":\"world\"}"}))

(deftest index-page-is-served
  (fact
    (http/get "http://localhost:3001/")
    =in=> {:status 200
           :headers {"Content-Type" "text/html; charset=utf-8"
                     "cache-control" "no-cache"
                     "etag" (re-pattern #"\d+")}})
  (let [etag (-> (http/get "http://localhost:3001/")
                 :headers
                 (get "etag"))]
    (fact
      (http/get "http://localhost:3001/" {:headers {"if-modified-since" etag}})
      =in=> {:status 304})
    (fact
      (http/get "http://localhost:3001/" {:headers {"if-modified-since" "foo"}})
      =in=> {:status 200})))

(deftest not-found-is-served
  (fact
    (http/get "http://localhost:3001/foozaa" {:throw-exceptions false})
    =in=> {:status 404}))

(deftest resources-are-served
  (fact
    (http/get "http://localhost:3001/asset/foo.css" {:throw-exceptions false})
    =in=> {:status 200
           :headers {"Content-Type" "text/css; charset=utf-8"
                     "cache-control" "no-cache"
                     "etag" (re-pattern #"\d+")}
           :body "* {\n  background: salmon;\n}\n\n"})
  (fact
    (http/get "http://localhost:3001/asset/foo.js" {:throw-exceptions false})
    =in=> {:status 200
           :headers {"Content-Type" "application/javascript; charset=utf-8"}})
  (fact
    (http/get "http://localhost:3001/asset/foo.txt" {:throw-exceptions false})
    =in=> {:status 200
           :headers {"Content-Type" "text/plain; charset=utf-8"}})
  (fact
    (http/get "http://localhost:3001/asset/foo.bar" {:throw-exceptions false})
    =in=> {:status 404})

  (let [etag (-> (http/get "http://localhost:3001/asset/foo.txt")
                 :headers
                 (get "etag"))]
    (fact
      (http/get "http://localhost:3001/asset/foo.txt" {:headers {"if-modified-since" etag}})
      =in=> {:status 304})
    (fact
      (http/get "http://localhost:3001/asset/foo.txt" {:headers {"if-modified-since" "foo"}})
      =in=> {:status 200})))
