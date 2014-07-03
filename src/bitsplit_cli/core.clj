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
    (loop []
        (let [command (read-in)]
            (if-not (exit? command)
                (do 
                    (execute {
                        :storage storage
                        :command command
                        :client client
                        }) 
                    (recur))))))

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

; A plan:
;  define a spec for the shell, then functions to handle each command
;  then, just stick things in a loop and you're good. some kind 
;  of rpc-daemonization would be dope, too

; the spec
; 
