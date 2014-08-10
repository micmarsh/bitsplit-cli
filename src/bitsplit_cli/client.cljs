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
    {(.getAddress account)
     (-> account .balance js/Number)})

(defn- send? [fee amount]
    (< (* 5 fee) amount))

(defprotocol Shutdown
  (shutdown! [this]))

(defrecord Client [wallet]
    Queries
    (addresses [this]
        (-> wallet .-addresses js->clj))
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
        (chans->chan
          (for [[from splits] amounts
                [to amount] splits
                :when (send? (.-fee coin) amount)]
            (let [account (aget (.-aaccounts coin) from)]
              (call-method coin "sendFrom" account to amount)))))
    (new-address! [this]
        (-> coin
            .createAccount
            .getAddress))
    Shutdown
    (shutdown! [this]
        (.close coin)))

(defn- change-dust! [coin]
    (set! (.-dust coin) 1)
    coin)

(defn new-client [location storage]
    (-> {:db
            {:type "tiny" :path
                (str location "tinydb")}
         :wallet
            (str location "wallet.json")}
         clj->js
         coined
         change-dust!
         (->Client storage)))

(fix-bcoin-issue!)