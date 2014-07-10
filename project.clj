(defproject bitsplit-cli "0.1.0"
  :description "A wrapper over a bitcoind instance"
  :url "http://github.com/micmarsh/bitsplit-cli"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojure "1.6.0"]
                 [bitsplit-core "0.1.6"]]

  :main bitsplit-cli.core

  :profiles {
    :dev {
        :dependencies [[org.clojure/clojurescript "0.0-2268"]]

        :plugins [[lein-cljsbuild "1.0.3"]
                  [lein-npm "0.4.0"]]

        :node-dependencies [[prompt "0.2.13"]]

        :cljsbuild {
          :builds [{
              :source-paths ["target/cljs"]
              :compiler {
                :output-to "target/main.js"
                :optimizations :whitespace
                :pretty-print true}}]}
    }
})
