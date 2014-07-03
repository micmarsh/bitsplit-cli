(ns bitsplit-cli.core
    (:use bitsplit.client.protocol
          bitsplit-cli.filesystem))

(defn -main [& args]
    (let [fs (->File "FAKE")]
        (println fs)))
