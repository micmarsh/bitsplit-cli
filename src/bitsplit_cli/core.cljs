(ns bitsplit-cli.core
    (:use [cljs.core.async :only (chan put! <!)]
          [bitsplit-cli.filesystem :only (->File)]
          [bitsplit-cli.commands :only (execute)])
    (:use-macros 
        [cljs.core.async.macros :only (go-loop)]))
(def prompt (js/require "prompt"))

(def storage (->File "FAKE"))
; (def client (->Bitcoind ""))

(defn node->channel [function]
    (fn [& args]
        (let [return (chan 1)
              into-chan 
                (fn [err result]
                    (if-not (nil? err)
                        (put! return {:error err})
                        (put! return result)))]
        (apply function (concat args [into-chan]))
        return)))

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
(apply -main 
    (-> js/process (.-argv) (.slice 2)))
