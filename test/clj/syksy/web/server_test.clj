(ns syksy.web.server-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [testit.core :refer :all]
            [integrant.core :as ig]
            [clj-http.client :as http]
            [jsonista.core :as json]
            [ring.util.http-response :as resp]
            [potpuri.core :as p]
            [syksy.core :as core]
            [syksy.web.index :as index]
            [syksy.web.server :as server]
            [syksy.web.resource :as resource]
            [syksy.web.resources :as resources]
            [syksy.web.redirect :as redirect]))

(def ^:dynamic system nil)

(def json-mapper (json/object-mapper {:encode-key-fn name
                                      :decode-key-fn keyword}))


(defn with-web-server [f]
  (let [ctx {:foo "bar"}
        config (p/deep-merge
                 (core/default-components {:index-body (index/index {:title "Syksy test"})
                                           :routes (fn [request]
                                                     (when (-> request :uri (= "/api"))
                                                       (resp/ok {:hello "world"
                                                                 :ctx (-> request :ctx)})))
                                           :ctx ctx
                                           :addon-handlers [(ig/ref [::resource/handler ::test])
                                                            (ig/ref [::resources/handler ::test])
                                                            (ig/ref [::redirect/handler ::test])]})
                 {[::server/server ::core/syksy] {:port 3001}
                  [::resource/handler ::test] {:match? "/root" :resource-name "root.txt"}
                  [::resources/handler ::core/syksy] {:asset-dir "server-test-resources"}
                  [::resources/handler ::test] {:asset-prefix "/addon"
                                                :asset-dir "addon-resources"}
                  [::redirect/handler ::test] {:from "/moved", :location "/here"}})]
    (ig/load-namespaces config)
    (let [s (ig/init config)]
      (try
        (binding [system s]
          (f))
        (finally
          (ig/halt! s))))))

(use-fixtures :once with-web-server)

(def opts {:throw-exceptions false
           :redirect-strategy :none
           :accept :json})

(deftest api-is-served
  (fact
    (-> (http/get "http://localhost:3001/api" opts)
        (update :body json/read-value json-mapper))
    =in=> {:status 200
           :headers {"Content-Type" "application/json; charset=utf-8"
                     "cache-control" "no-store, must-revalidate"
                     "vary" "accept-encoding"}
           :body {:hello "world"
                  :ctx {:foo "bar"}}}))

(deftest index-page-is-served
  (fact
    (http/get "http://localhost:3001/" opts)
    =in=> {:status 200
           :headers {"Content-Type" "text/html; charset=utf-8"
                     "cache-control" "no-cache"
                     "etag" (re-pattern #"\d+")}
           :body (p/fn-> (str/index-of "title>Syksy test</title>") pos?)})
  (let [etag (-> (http/get "http://localhost:3001/" opts)
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
    (http/get "http://localhost:3001/foozaa" opts)
    =in=> {:status 404}))

(deftest resource-is-served
  (fact
    (http/get "http://localhost:3001/root" opts)
    =in=> {:status 200
           :headers {"Content-Type" "text/plain; charset=utf-8"
                     "cache-control" "no-cache"
                     "etag" (re-pattern #"\d+")}
           :body "Root"}))

(deftest resources-are-served
  (fact
    (http/get "http://localhost:3001/asset/foo.css" opts)
    =in=> {:status 200
           :headers {"Content-Type" "text/css; charset=utf-8"
                     "cache-control" "no-cache"
                     "etag" (re-pattern #"\d+")}
           :body "* {\n  background: salmon;\n}\n\n"})
  (fact
    (http/get "http://localhost:3001/asset/foo.js" opts)
    =in=> {:status 200
           :headers {"Content-Type" "application/javascript; charset=utf-8"}})
  (fact
    (http/get "http://localhost:3001/asset/foo.txt" opts)
    =in=> {:status 200
           :headers {"Content-Type" "text/plain; charset=utf-8"}})
  (fact
    (http/get "http://localhost:3001/asset/foo.bar" opts)
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

(deftest addon-resources-are-served
  (fact
    (http/get "http://localhost:3001/addon/bar.txt" opts)
    =in=> {:status 200
           :body "Hullo"}))

(deftest redirect-is-observed
  (fact
    (http/get "http://localhost:3001/moved" opts)
    =in=> {:status 307
           :headers {"location" "/here"}}))
