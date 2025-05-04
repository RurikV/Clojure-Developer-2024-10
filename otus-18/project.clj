(defproject otus-18 "0.1.0-SNAPSHOT"
  :description "Pokemon application with hexagonal architecture"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.673"]
                 [clj-http "3.12.3"]
                 [clj-http-fake "1.0.4"]
                 [cheshire "5.11.0"]
                 [com.github.seancorfield/next.jdbc "1.3.874"]
                 [com.github.seancorfield/honeysql "2.4.1026"]
                 [ragtime "0.8.1"]
                 [org.postgresql/postgresql "42.6.0"]

                 ;; Duct and Integrant dependencies
                 [integrant "0.8.1"]
                 [duct/core "0.8.0"]
                 [duct/module.logging "0.5.0"]
                 [duct/module.sql "0.6.1"]]

  :plugins [[duct/lein-duct "0.12.3"]]

  :main ^:skip-aot otus-18.main

  :resource-paths ["resources" "target/resources"]

  :profiles
  {:dev [:project/dev :profiles/dev]
   :profiles/dev {}
   :project/dev {:source-paths ["dev/src"]
                 :resource-paths ["dev/resources"]
                 :dependencies [[integrant/repl "0.3.3"]
                               [eftest "0.5.9"]
                               [kerodon "0.9.1"]
                               [hawk "0.2.11"]
                               [clojure.java-time "1.2.0"]
                               [org.clojure/tools.namespace "1.4.4"]
                               [fipp "0.6.26"]]}
   :uberjar {:prep-tasks ["javac" "compile" ["run" ":duct/compiler"]]
             :aot :all
             :uberjar-name "otus-18.jar"}})
