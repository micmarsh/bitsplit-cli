(ns bitsplit-cli.core
    (:use bitsplit.client.protocol
          bitsplit.storage.protocol))

(defn foo
  "I don't do a whole lot."
  [& x]
  (println x "Hello, World!"))

(def -main foo)
