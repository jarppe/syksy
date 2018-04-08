(defproject jarppe/syksy "0.0.1"
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]

                 [integrant "0.6.3"]
                 [integrant/repl "0.3.1"]

                 [org.immutant/web "2.1.10"]
                 [ring/ring-core "1.6.3"]
                 [commons-io "2.6"]
                 [metosin/ring-http-response "0.9.0"]
                 [metosin/muuntaja "0.5.0"]
                 [metosin/jsonista "0.1.1"]
                 [hiccup "2.0.0-alpha1"]

                 [org.clojure/tools.logging "0.4.0"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [org.slf4j/jul-to-slf4j "1.7.25"]
                 [org.slf4j/log4j-over-slf4j "1.7.25"]
                 [ch.qos.logback/logback-classic "1.2.3" :exclusions [org.slf4j/slf4j-api]]]

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :profiles {:dev {:resource-paths ["test-resources"]
                   :dependencies [[metosin/testit "0.2.1"]
                                  [clj-http "3.8.0"]
                                  [metosin/potpuri "0.5.1"]]}})
