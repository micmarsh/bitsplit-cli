(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          bitsplit.client.protocol
          bitsplit.storage.protocol
          bitsplit-cli.commands
          bitsplit-cli.bitcoind
          bitsplit-cli.filesystem)
    (:use-macros 
        [cljs.core.async.macros :only (go-loop)]))

(def prompt (js/require "prompt"))

(def storage (->File "FAKE"))
; (def client (->Bitcoind ""))



(defn read-prompt [p]
    (let [return (chan 1)]
        (.get prompt p
            (fn [err input]
                (put! return 
                    (aget input p))))
        return))

(def read-in (partial read-prompt "bitsplit> "))

(defn exit? [command]
    (->> "exit"
          seq
          (= (take 4 command))))

(defn start-repl []
    (execute {
        :storage storage
        :command "list"
        ; :client client
        })
    (go-loop [command (<! (read-in))]
        (if-not (exit? command)
            (do 
                (execute {
                    :storage storage
                    :command command
                    :client client
                    }) 
                (recur (<! (read-in)))))))

(defn -main [& args]
    (if (empty? args)
        (start-repl) 
        (execute {
            :storage storage
            :command (->> args
                        (map #(str % " "))
                        (apply str))
            ; :client client
            })))
