(ns bitsplit-cli.core
    (:use bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.commands
          bitsplit-cli.bitcoind
          bitsplit-cli.filesystem))

(defn read-prompt [prompt]
    (print prompt)
    (flush)
    (read-line))

(def read-in (partial read-prompt "bitsplit> "))

(defn start-repl []
    (loop []
        (let [commmand (read-in)]
            (if-not (= commmand "exit")
                (do 
                    (println "sup" commmand) 
                    (recur))))))

(defn -main [& args]
    (if (empty? args) 
        (println "empty" args)
        (start-repl)))

; A plan:
;  define a spec for the shell, then functions to handle each commmand
;  then, just stick things in a loop and you're good. some kind 
;  of rpc-daemonization would be dope, too

; the spec
; 
