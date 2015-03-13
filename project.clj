(defproject clojure-benchmarking "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [criterium "0.4.3"]
                 [manifold "0.1.0-beta10"]
                 [incanter "1.5.5"]]
  :plugins [[lein-nodisassemble "0.1.3"]]
  :main clojure-benchmarking.control-benchmark
  ;:aot [clojure-benchmarking.control-benchmark]
  )
