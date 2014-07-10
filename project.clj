(defproject bitsplit-cli "0.1.0"
  :description "A wrapper over a bitcoind instance"
  :url "http://github.com/micmarsh/bitsplit-cli"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [org.clojure/clojure "1.6.0"]
                 [bitsplit-core "0.1.6"]

                 ;clj-btc
                 [http-kit "2.1.11"]
                 [org.clojure/data.json "0.2.5"]]

  :main bitsplit-cli.core)
