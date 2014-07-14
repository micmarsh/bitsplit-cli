(ns bitsplit-cli.client
    (:use
        [cljs.core.async :only (chan put! close!)]
        [bitsplit.client.protocol :only 
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit-cli.constants :only (DIR)]
        [bitsplit-cli.utils :only (call-method chans->chan)]))
(def coined (js/require "coined"))

(defn fix-bcoin-issue! []
    (let [emitter (.-EventEmitter (js/require "events"))
          old-emit (-> emitter .-prototype .-emit)]
        (set! (-> emitter .-prototype .-emit)
            (fn [& args]
                (this-as self
                    (when (not= (first args) "error")
                        (.apply old-emit self 
                            (into-array args))))))))

(defn account->amount [account]
    (println (.getAddress account) (.balance account))
    {(.getAddress account)
     (-> account .balance js/Number)})

(defn empty-chan []
    (let [c (chan)] 
        (close! c) 
        c))

(defrecord Client [coin]
    Queries
    (addresses [this]
        (->> coin
            .-aaccounts
            (.keys js/Object)
            js->clj))
    (unspent-amounts [this]
        (let [unspent (->> coin .-accounts (map account->amount))]
            (apply merge unspent)))
    (unspent-channel [this]
        (let [return (chan)]
            (js/setInterval 
                (fn []
                    (let [unspent (unspent-amounts this)]
                        (put! return unspent)))
                10000)
            return))
    Operations
    (send-amounts! [this amounts] 
        (println (unspent-amounts this))
        (println amounts)
        ((comp chans->chan  map)
            (fn [[address amount]]
                (if (= amount 0)
                    (empty-chan)
                    (call-method coin "sendTo" address (- amount 100))))
            amounts))
    (new-address! [this]
        (-> coin
            .createAccount
            .getAddress)))

(defn new-client [location]
    (-> {:db  
            {:type "tiny" :path 
                (str location "tinydb")}
         :wallet 
            (str location "wallet.json")}
         clj->js
         coined
         ->Client))

(fix-bcoin-issue!)