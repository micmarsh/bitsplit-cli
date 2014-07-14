(ns bitsplit-cli.client
    (:use
        [cljs.core.async :only (chan put! close!)]
        [bitsplit.client.protocol :only 
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit.storage.protocol :only (all)]
        [bitsplit-cli.constants :only (DIR)]
        [bitsplit-cli.utils :only (call-method chans->chan empty-chan)]))
(def coined (js/require "coined"))

(defn- fix-bcoin-issue! []
    (let [emitter (.-EventEmitter (js/require "events"))
          old-emit (-> emitter .-prototype .-emit)]
        (set! (-> emitter .-prototype .-emit)
            (fn [& args]
                (this-as self
                    (when (not= (first args) "error")
                        (.apply old-emit self 
                            (into-array args))))))))

(defn- account->amount [account]
    (println (.getAddress account) (.balance account))
    {(.getAddress account)
     (-> account .balance js/Number)})

(defn- -find-account [address accounts]
    (->> accounts
        (filter #(= (.getAddress %) address))
        first))

(defn- has-addr? [to [address splits]] 
    (contains? splits to))

(defprotocol WithStorage 
    (find-account [this send-to]))

(defprotocol Shutdown
    (shutdown! [this]))

(defrecord Client [coin storage]
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
                5000)
            return))
    Operations
    (send-amounts! [this amounts] 
        (println (unspent-amounts this))
        (println amounts)
        ((comp chans->chan  map)
            (fn [[address amount]]
                (let [from (find-account this address)]
                    (if (= amount 0)
                        (empty-chan)
                        (call-method coin "sendFrom" from address amount))))
            amounts))
    (new-address! [this]
        (-> coin
            .createAccount
            .getAddress))

    WithStorage
    (find-account [this send-to]
        (let [address (->> storage all (filter (partial has-addr? send-to)) ffirst)
              account (-find-account address (.-accounts coin))]
              account))
    Shutdown
    (shutdown! [this]
        (.close coin)))

(defn new-client [location storage]
    (-> {:db  
            {:type "tiny" :path 
                (str location "tinydb")}
         :wallet 
            (str location "wallet.json")}
         clj->js
         coined
         (->Client storage)))

(fix-bcoin-issue!)