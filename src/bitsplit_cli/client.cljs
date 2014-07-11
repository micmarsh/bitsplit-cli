(ns bitsplit-cli.client
    (:use
        [cljs.core.async :only (chan put!)]
        [bitsplit.client.protocol :only 
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit-cli.filesystem :only (DIR)]))
(def coined (js/require "coined"))

(def fake-addresses (atom
    ["dke98di398fdjr98feijr3oifsoij"
     "kdi9d9ekdkjeufjeueudjudd"
     "83dkm3mde030dk3kd3jdmjewkdi"]))
(def findex (atom 0))

(defrecord FakeClient [info]
    Queries
    (addresses [this] @fake-addresses)
    (unspent-amounts [this]
        (vec 
            (map (fn [address amount] {"address" address "amount" amount})
            (addresses this) [0.23 2 1.8])))
    (unspent-channel [this]
        (let [amounts (unspent-amounts this)
              index (atom 0)
              return (chan)
              inc-mod (comp #(mod % 3) inc)]
            (js/setInterval
                (fn []
                    (put! return 
                        (amounts (swap! index inc-mod))))
                1000)
            return))
    Operations
    (send-amounts! [this amounts] (println "sending" amounts))
    (new-address! [this]
        (swap! fake-addresses conj 
            (str "newest-address" (swap! findex inc)))))

(defn fix-bcoin-issue! []
    (let [emitter (.-EventEmitter (js/require "events"))
          old-emit (-> emitter .-prototype .-emit)]
        (set! (-> emitter .-prototype .-emit)
            (fn [& args]
                (this-as self
                    (when (not= (first args) "error")
                        (.apply old-emit self 
                            (into-array args))))))))

(defrecord Client [coin]
    Queries
    (addresses [this]
        (->> coin
            .-aaccounts
            (.keys js/Object)
            js->clj))
    (unspent-amounts [this]
        (let [unspent (-> coin .-accounts (aget 0) .unspent)]
            unspent))
    (unspent-channel [this]
        (let [return (chan)]
            (js/setInterval 
                (fn []
                    (println "interval")
                    (let [unspent (unspent-amounts this)]
                        (put! return unspent)))
                1000)
            return))
    Operations
    (send-amounts! [this amounts] (println "sending" amounts))
    (new-address! [this]
        (swap! fake-addresses conj 
            (str "newest-address" (swap! findex inc)))))

(defn new-client [name]
    (-> {:db  
            {:type "tiny" :path "tinydb"}
         :wallet "coined.json"}
         clj->js
         coined
         ->Client))

(fix-bcoin-issue!)