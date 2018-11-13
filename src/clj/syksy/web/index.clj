(ns syksy.web.index
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [ring.util.http-response :as resp]
            [hiccup.core :as hiccup]
            [hiccup.page :as page]
            [syksy.web.cache :as cache]
            [syksy.util.checksum :as checksum])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)
           (java.util.zip GZIPOutputStream)
           (java.nio.charset StandardCharsets)))

;
; Note about the JavaScript loading:
;
; Assumes that the application JS is at /asset/js/main.js. Application JS is loaded at the end
; of the HTML body. Other additional JS are declared at the HTML head.
;
; By default the `script` tags have `deref` (https://developer.mozilla.org/en-US/docs/Web/HTML/Element/script#attr-defer)
; attribute set. This instructs the browser to fetch JS in background and in parallel, but ensures that they
; are executed in the order of they are declared.
;
; Note that IE9 has serious bugs (https://github.com/h5bp/lazyweb-requests/issues/42#issuecomment-1901803) with
; `deref` that can cause the scripts the be executed in random order. If you need to support IE9, set `deref?`
; option to false.
;

;;
;; Index page:
;;

(defn index [{:keys [title loading favicon css js deref?]
              :or   {title   "Another fancy Syksy app"
                     loading "Loading..."
                     favicon "/asset/favicon.ico"
                     deref?  true}}]
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
                 :rel  "stylesheet"}])
       (for [src js]
         [:script {:src   src
                   :type  "text/javascript"
                   :deref deref?}])]
      [:body
       [:div#app
        [:h1.loading loading]]
       [:script {:src   "/asset/js/main.js"
                 :type  "text/javascript"
                 :deref deref?}]])))

;;
;; Index handler:
;;


(defn- gzip ^bytes [^bytes data]
  (let [in   (ByteArrayInputStream. data)
        out  (ByteArrayOutputStream.)
        gzip (GZIPOutputStream. out)]
    (io/copy in gzip)
    (.close gzip)
    (.toByteArray out)))


(defn- if-none-match? [request etag]
  (-> request
      :headers
      (get cache/if-none-match)
      (= etag)))


(defn- accept-gzip? [request]
  (some-> request
          :headers
          (get "accept-encoding")
          (str/includes? "gzip")))


(defn make-index-handler [^String index-body]
  (let [etag           (-> index-body
                           (checksum/hash-string)
                           (str))
        basic-response (-> index-body
                           (resp/ok)
                           (resp/content-type "text/html; charset=utf-8")
                           (resp/header "ETag" etag)
                           (resp/header cache/cache-control cache/cache-control-no-cache))
        gzip-body      (-> index-body
                           (.getBytes StandardCharsets/UTF_8)
                           (gzip))
        gzip-response  (-> (resp/ok)
                           (resp/content-type "text/html; charset=utf-8")
                           (resp/header "ETag" etag)
                           (resp/header cache/cache-control cache/cache-control-no-cache)
                           (resp/header "Content-Encoding" "gzip"))
        not-modified   (resp/not-modified)]
    (fn [request]
      (cond
        (if-none-match? request etag) not-modified
        (accept-gzip? request) (assoc gzip-response :body (ByteArrayInputStream. gzip-body))
        :else basic-response))))
