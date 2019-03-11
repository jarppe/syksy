(ns syksy.web.router
  (:require [integrant.core :as ig]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [muuntaja.core]
            [muuntaja.interceptor]
            [ring.util.http-response :as resp]
            [reitit.http :as http]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.http.coercion :as coercion]
            [reitit.coercion.schema :as schema-coercion]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.exception :as exception]
            [reitit.interceptor.sieppari :as sieppari]
            [syksy.web.cache :as cache]
            [syksy.web.index :as index]
            [syksy.web.cors :as cors]))


(def exception-handlers
  (assoc exception/default-handlers
    :ring.util.http-response/response (fn [e _]
                                        (-> e ex-data :response))
    :core.util.fail/fail (fn [e _]
                           (log/error e "failure:" (-> e ex-data pr-str))
                           (resp/internal-server-error {:message "unexpected internal failure"}))
    ::exception/default (fn [e _]
                          (log/error e "unexpected error")
                          (resp/internal-server-error {:message "unexpected internal error"}))))


(def default-status-interceptor
  {:name  :default-status
   :leave (fn [ctx]
            (if (-> ctx :response :status)
              ctx
              (update ctx :response assoc :status 200)))})


(def base-interceptors [(cache/cache-interceptor)
                        (cors/cors-interceptor)
                        (parameters/parameters-interceptor)
                        (muuntaja/format-negotiate-interceptor)
                        (muuntaja/format-response-interceptor)
                        (exception/exception-interceptor exception-handlers)
                        (muuntaja/format-request-interceptor)
                        (coercion/coerce-response-interceptor)
                        (coercion/coerce-request-interceptor)
                        default-status-interceptor])


(defn- command-name->route-name [command-name]
  (str (as-> command-name n (namespace n) (str/split n #"\.") (str/join "/" n))
       "/"
       (-> command-name (name))))


(defn- command->route [command]
  (let [method         (-> command :method (or :post))
        parameters-key (case method
                         :post :body
                         :get :query)]
    [(-> command :name (command-name->route-name))
     {:name       (->> command :name)
      :parameters (when-some [request-schema (-> command :request)]
                    {parameters-key request-schema})
      :responses  (when-some [response-schema (-> command :response)]
                    {200 {:body response-schema}})
      method      {:summary (-> command :summary)
                   :handler (-> command :handler)}
      :command    command}]))


(defmethod ig/init-key :syksy.web/router [_ {:keys [index-body resource-handler interceptors commands]}]
  (let [index-redirect     {:get {:handler (constantly (resp/temporary-redirect "/web/")), :no-doc true}}
        index-route        {:get {:handler (index/make-index-handler index-body), :no-doc true}}
        resource-route     {:get {:handler resource-handler, :no-doc true}}
        swagger-json-route {:get {:handler (swagger/create-swagger-handler), :no-doc true, :swagger {:info {:title "API"}}}}]
    (http/ring-handler
      (http/router
        [""
         ["/" index-redirect]
         ["/web" index-redirect]
         ["/web/*" index-route]
         ["/asset/*resource" resource-route]
         ["/swagger.json" swagger-json-route]
         ["/api/" (mapv command->route commands)]]
        {:data {:coercion     schema-coercion/coercion
                :muuntaja     muuntaja.core/instance
                :interceptors (concat base-interceptors interceptors)}})
      (some-fn (swagger-ui/create-swagger-ui-handler {:path "/swagger", :url "/swagger.json"})
               (constantly (resp/not-found "The will is strong, but I can't understand you.")))
      {:executor sieppari/executor})))
