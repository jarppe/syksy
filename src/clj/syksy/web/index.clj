(ns syksy.web.index
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [ring.util.http-response :as resp]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [syksy.web.cache :as cache]
            [syksy.util.checksum :as checksum]
            [clojure.string :as str]))

;;
;; Index page:
;;

(defn index [{:keys [title loading favicon css js]
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
       (for [href (conj css "/asset/css/style.css")]
         [:link {:href href
                 :type "text/css"
                 :rel "stylesheet"}])
       (for [src js]
         [:script {:src src
                   :type "text/javascript"
                   :deref true}])]
      [:body
       [:div#app
        [:h1.loading loading]]
       [:script {:src "/asset/js/main.js"
                 :type "text/javascript"
                 :deref true}]])))

;;
;; Index handler:
;;

(defmethod ig/init-key ::handler [_ {:keys [index-body uri-match? uri-prefix]}]
  (let [index-hash (-> index-body (or (index {})) checksum/hash-string str)
        index-response (-> index-body
                           (resp/ok)
                           (resp/content-type "text/html; charset=utf-8")
                           (resp/header "etag" index-hash)
                           (resp/header cache/cache-control cache/cache-control-no-cache))
        not-modified (resp/not-modified)
        uri-match? (or uri-match?
                       (if uri-prefix
                         (fn [uri] (str/starts-with? uri uri-prefix))
                         (fn [uri] (= uri "/"))))]
    (log/infof "index handler: index-hash=%s" index-hash)
    (fn [request]
      (when (and (-> request :request-method (= :get))
                 (-> request :uri uri-match?))
        (if (-> request :headers (get cache/if-none-match) (= index-hash))
          not-modified
          index-response)))))
