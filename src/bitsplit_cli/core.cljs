(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          [bitsplit-cli.filesystem :only (->File)]
          [bitsplit-cli.commands :only (execute)])
    (:use-macros 
        [cljs.core.async.macros :only (go-loop)]))
(def prompt (js/require "prompt"))
(set! (.-message prompt) "")
(set! (.-delimiter prompt) "")
(set! (.-colors prompt) false)

(def storage (->File "FAKE"))
; (def client (->Bitcoind ""))

(defn- read-prompt [message]
    (let [return (chan 1)]
        (.get prompt message
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
    (.start prompt)
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
                    ; :client client
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

(set! *main-cli-fn* -main)
