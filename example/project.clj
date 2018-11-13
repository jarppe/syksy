(defproject syksy-example "0.0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [jarppe/syksy "0.0.5-SNAPSHOT"]

                 ; ClojureScript:
                 [org.clojure/clojurescript "1.10.439"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [com.cognitect/transit-cljs "0.8.256"]]

  :source-paths ["src/clj" "src/cljc" "src/cljs"]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-figwheel "0.5.16"]
            [lein-pdo "0.1.1"]
            [deraen/lein-sass4clj "0.3.1"]]

  :sass {:source-paths ["core/src/sass" "src/sass"]
         :source-map   true
         :output-style :compressed}

  :figwheel {:css-dirs ["target/dev/resources/public/css"]
             :repl     false}

  :target-path "target/dev"
  :auto-clean false

  :profiles {:dev {:source-paths ["src/dev"]
                   :dependencies [[integrant/repl "0.3.1"]]}
             :uberjar {:target-path    "target/prod"
                       :source-paths   ^:replace ["src/clj" "src/cljc" "src/cljs"]
                       :resource-paths ^:replace ["resources" "target/prod/resources"]
                       :sass           {:target-path "target/prod/resources/public/css"
                                        :source-map  false}
                       :main           syksy.main
                       :aot            :all
                       :uberjar-name   "app.jar"}}

  :cljsbuild {:builds [{:id           "dev"
                        :figwheel     true
                        :compiler     {:main            app.front.init
                                       :asset-path      "/asset/js/out"
                                       :output-to       "target/dev/resources/public/js/main.js"
                                       :output-dir      "target/dev/resources/public/js/out"
                                       :preloads        [devtools.preload
                                                         day8.re-frame-10x.preload]
                                       :closure-defines {goog.DEBUG                            true
                                                         "re_frame.trace.trace_enabled_QMARK_" true}
                                       :external-config {:devtools/config {:features-to-install [:formatters :hints]}}
                                       :optimizations   :none
                                       :source-map      true
                                       :cache-analysis  true
                                       :pretty-print    true
                                       :parallel-build  true}
                        :source-paths ["src/cljc" "src/cljs"]}
                       {:id           "prod"
                        :compiler     {:main             app.front.init
                                       :output-to        "target/prod/resources/public/js/main.js"
                                       :closure-defines  {goog.DEBUG false}
                                       :parallel-build   true
                                       :optimizations    :advanced
                                       :closure-warnings {:non-standard-jsdoc :off}}
                        :source-paths ["src/cljc" "src/cljs"]}]}

  :aliases {"dev"     ["do"
                       ["clean"]
                       ["pdo"
                        ["sass4clj" "auto"]
                        ["figwheel"]]]
            "uberjar" ["with-profile" "uberjar" "do"
                       ["clean"]
                       ;["sass4clj" "once"]
                       ;["cljsbuild" "once" "prod"]
                       ["uberjar"]]})
