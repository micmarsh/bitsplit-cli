(ns bitsplit-cli.core
    (:use bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.filesystem))

(defn -main [& args]
    (let [fs (->File "FAKE")]
        (save! fs "address1" {"address2" 8})
        (save! fs "yo" {"address1" 2})
        (println (lookup fs "yo"))))
