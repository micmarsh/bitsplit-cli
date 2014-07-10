(ns bitsplit-cli.core
    (:use bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.commands
          bitsplit-cli.bitcoind
          bitsplit-cli.filesystem))


(def storage (->File "FAKE"))
(def client (->Bitcoind ""))

(defn read-prompt [prompt]
    (print prompt)
    (flush)
    (read-line))

(def read-in (partial read-prompt "bitsplit> "))

(defn exit? [command]
    (->> "exit"
          seq
          (= (take 4 command))))

(defn start-repl []
    (execute {
        :storage storage
        :command "list"
        :client client
        })
    (loop [command (read-in)]
        (if-not (exit? command)
            (do 
                (execute {
                    :storage storage
                    :command command
                    :client client
                    }) 
                (recur (read-in))))))

(defn -main [& args]
    (if (empty? args)
        (start-repl) 
        (execute {
            :storage storage
            :command (->> args
                        (map #(str % " "))
                        (apply str))
            :client client
            })))
