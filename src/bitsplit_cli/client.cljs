(ns bitsplit-cli.client
    (:use
        [bitsplit.client.protocol :only
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit.storage.protocol :only (all)]
        [bitsplit-cli.constants :only (DIR)]
        [bitsplit-cli.utils :only (call-method)])
    (:require [cljs.core.async :as a]))
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
    (< (* 3 fee) amount))

(defprotocol Shutdown
  (shutdown! [this]))

(defrecord Client [wallet]
    Queries
    (addresses [this]
        (-> wallet .-addresses js->clj))
    (unspent-amounts [this]
      (throw
        (js/Error. "Synchronous unspent-amounts not a thing")))
    (unspent-channel [this]
        (let [return (a/chan)]
            (js/setInterval
                (fn []
                    (let [unspent (unspent-amounts this)]
                        (a/put! return unspent)))
                5000)
            return))
    Operations
    (send-amounts! [this amounts]
        (a/merge
          (for [[from splits] amounts
                [to amount] splits
                :when (send? (.-fee coin) amount)]
            (let [account (aget (.-aaccounts coin) from)]
              (println from to amount)
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

(defn setup-save-loop! [coin]
  (js/setInterval
    #(.saveWallet coin) 10000)
  coin)

(defn new-client [location]
    (-> {:db
          {:type "tiny" :path
            (str location "tinydb")}
         :wallet
            (str location "wallet.json")}
         clj->js
         coined
         change-dust!
         setup-save-loop!
         (->Client)))

(fix-bcoin-issue!)