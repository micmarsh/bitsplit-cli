(ns bitsplit-cli.client
    (:require [bitsplit-cli.client.network :refer (address->unspents urls)]
              [bitsplit.client.protocol :refer
                (Queries addresses unspent-amounts unspent-channel
                 Operations send-amounts! new-address!)]
              [cljs.core.async :as a])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(def Transaction
  (.-Transaction (js/require "bitcoinjs-lib")))

(defn new-tx [[address unspents]]
  (let [tx (Transaction.)]
    (doseq [{:keys [tx-hash index]} unspents]
      (println tx-hash index unspents)
      (.addInput tx tx-hash index))
    tx))

(defn- send? [fee amount]
    (< (* 3 fee) amount))

(defn- safe-put! [channel item]
  (println "yay safe putting" item)
  (when-not (nil? item)
    (a/put! channel item)))

(defrecord Client [wallet]
    Queries
    (addresses [this]
        (-> wallet .-addresses js->clj))
    (unspent-amounts [this]
      (let [my-addrs (addresses this)]
        (println "our addresses" my-addrs)
        (if (empty? my-addrs)
          (go nil)
          (->> my-addrs
            (map (partial address->unspents urls))
            (a/merge)
            (a/into { })))))
    (unspent-channel [this]
      (let [return (a/chan)]
        (js/setInterval
          #(let [unspents (unspent-amounts this)]
            (a/take! unspents
              (partial safe-put! return)))
          10000)
        return))
    Operations
    (send-amounts! [this info]
      (let [{:keys [percentages unspents]} info
            txs (map new-tx unspents)]
        (println "yay some new txs" txs)
        ; TODO here
        ;  * use unspents->amounts to get a more traditiaonl {addr amount} thingy
        ;  * can use stuff from core.calculcate to build thingy of address to send to
        ;  * then, will be able to add appropriate output address and amount for each tx
        ;  * notes on that: should group txs by address, so can sign w/ correct private keys
        ;    when the right time comes. according to examples, once for each input (thumbsup)
        ))
    (new-address! [this] ))


(defn new-client [location]
    (->Client #js {:addresses
        #js ["1LXv8VR7XMaCNAqui9hUicfsZqs4bGFpX4"
             "19eprtsSudARY78i9WRegjmG5DW5XTMZ4S"]}))
