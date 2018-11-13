(defproject jarppe/syksy "0.0.5-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]

                 [integrant "0.7.0"]

                 [org.immutant/web "2.1.10"]
                 [ring/ring-core "1.7.1"]
                 [commons-io "2.6"]
                 [metosin/ring-http-response "0.9.1"]
                 [metosin/muuntaja "0.6.1"]
                 [metosin/maailma "1.1.0"]
                 [com.cognitect/transit-clj "0.8.313"]
                 ; Explicit dep to this version to avoid conflicts from buddy and others:
                 [com.fasterxml.jackson.core/jackson-databind "2.9.7"]
                 [hiccup "2.0.0-alpha1"]

                 ; Use more recent xnio to avoid warnings on JRE 11:
                 [org.jboss.xnio/xnio-api "3.6.5.Final"]
                 [org.jboss.xnio/xnio-nio "3.6.5.Final"]

                 ; Routing:
                 [metosin/reitit "0.2.7"]
                 [metosin/reitit-interceptors "0.2.7"]
                 [metosin/reitit-schema "0.2.7"]
                 [metosin/reitit-swagger "0.2.7"]
                 [metosin/reitit-swagger-ui "0.2.7"]

                 ; Logging:
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]
                 [org.jboss.logging/jboss-logging "3.3.2.Final"]

                 ; Logging:
                 [org.clojure/tools.logging "0.4.1"]
                 [org.slf4j/slf4j-api "1.7.25"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]]

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]

  :profiles {:dev {:source-paths ["src/dev" "example/src/clj" "example/src/cljc" "example/src/cljs"]
                   :resource-paths ["test-resources" "example/resources"]
                   :dependencies [[eftest "0.5.3"]
                                  [metosin/testit "0.4.0-SNAPSHOT"]
                                  [clj-http "3.9.1"]
                                  [metosin/potpuri "0.5.1"]
                                  [integrant/repl "0.3.1"]
                                  [com.7theta/re-frame-fx "0.2.1"]]}
             :jar {:aot :all
                   :main syksy.main}}

  :plugins [[lein-eftest "0.5.3"]
            [lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]
            [lein-pdo "0.1.1"]
            [deraen/lein-sass4clj "0.3.1"]]

  :eftest {:multithread? false}
  :test-selectors {:default     (constantly true)
                   :all         (constantly true)
                   :integration :integration
                   :unit        (complement :integration)}

  :sass {:source-paths ["core/src/sass" "src/sass"]
         :source-map   true
         :output-style :compressed}

  :figwheel {:css-dirs ["target/dev/resources/public/css"]
             :repl     false}


  :target-path "target/dev"
  :auto-clean false
  :min-lein-version "2.8.1")
