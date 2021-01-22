(defproject arcian "0.0.1-SNAPSHOT"
  :description "An alternative key remapping tool for Razer Tartarus V2 (Synapse)"
  :url "https://github.com/sandmark/arcian"
  :min-lein-version "2.0.0"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [net.java.dev.jna/jna "5.6.0"]
                 [com.taoensso/timbre "5.1.0"]
                 [clj-native "0.9.5"]]
  :main ^:skip-aot arcian.core
  :target-path "target/%s"
  :resource-paths ["resources" "target/resources"]
  :profiles
  {:dev     {:source-paths   ["dev/src"]
             :resource-paths ["dev/resources"]}
   :repl    {:prep-tasks   ^:replace ["javac" "compile"]
             :repl-options {:init-ns user}}
   :uberjar {:aot :all}})
