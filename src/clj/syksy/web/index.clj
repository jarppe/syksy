(ns syksy.web.index
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [syksy.web.cache :as cache]
            [syksy.util.checksum :as checksum]))

;;
;; Index page:
;;

(defn index [{:keys [title loading favicon]
              :or {title "Another fancy Syksy app"
                   loading "Loading..."
                   favicon "/asset/favicon.ico"}}]
  (hiccup/html
    (page/html5
      [:head
       [:title title]
       [:meta {:charset "utf-8"}]
       [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
       [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}]
       [:link {:href favicon :rel "icon"}]
       (page/include-css "/asset/css/style.css")]
      [:body
       [:div#app
        [:div.loading
         [:h1 loading]]]
       [:div#dev]
       (page/include-js "/asset/js/main.js")])))

;;
;; Index handler:
;;

(defmethod ig/init-key ::handler [_ {:keys [index-body] :as opts}]
  (let [index-hash (-> index-body checksum/hash-string str)
        index-response (-> index-body
                           (resp/ok)
                           (resp/content-type "text/html; charset=utf-8")
                           (resp/header "etag" index-hash)
                           (resp/header cache/cache-control cache/cache-control-no-cache))
        not-modified (resp/not-modified)]
    (log/infof "index handler: index-hash=%s" index-hash)
    (fn [request]
      (when (and (-> request :request-method (= :get))
                 (-> request :uri (= "/")))
        (if (-> request :headers (get cache/if-modified-since) (= index-hash))
          not-modified
          index-response)))))
