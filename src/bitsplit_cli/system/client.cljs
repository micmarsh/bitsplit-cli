(ns bitplit-cli.system.client
    (:require [bitsplit.client.protocol :refer
                (Queries addresses unspent-amounts unspent-channel
                 Operations send-amounts! new-address!)]
              [bitsplit.utils.calculate :refer (apply-percentages)]
              [bitplit-cli.system.client.network :refer
                (address->unspents push-tx)]
              [bitplit-cli.system.client.transactions :as tx]
              [bitplit-cli.system.client.wallet :as wallet]
              [bitsplit-cli.utils.async :refer (empty-chan)]
              [cljs.core.async :as a])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(defn- unspents->amounts [unspents]
  (into { }
    (map (fn [[address txs]]
      [address
        (->> txs (map :value) (reduce +))])
    unspents)))

(defn- send? [fee amount]
    (< (* 3 fee) amount))

(defn- safe-put! [channel item]
  (when-not (nil? item)
    (a/put! channel item)))

(defn- address->private  [wallet address]
  (.getPrivateKeyForAddress wallet address))

(defn- private-keys [wallet addresses]
  (let [privates (map (partial address->private wallet) addresses)]
    (zipmap addresses privates)))

(defn- unspents? [unspents]
  (->> unspents
    (map second)
    (remove empty?)
    (empty?)
    (not)))

(defrecord Client [location]
  Queries
    (addresses [this]
      (-> location
            wallet/load-wallet
            .-addresses
            js->clj))
    (unspent-amounts [this]
      (let [my-addrs (addresses this)]
        (if (empty? my-addrs)
          (empty-chan)
          (->> my-addrs
               (map (partial address->unspents :test))
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
            addrs (addresses this)
            amounts (unspents->amounts unspents)
            totals (apply-percentages percentages amounts)
            txs (tx/make-txs addrs)
            keys (private-keys (wallet/load-wallet location) addrs)]
        (when (unspents? unspents)
          (tx/with-inputs! txs unspents)
          (tx/with-outputs! txs totals)
          (tx/with-signatures! txs keys)
          (->> txs
               (vals)
               (map (partial push-tx :test))
               (a/merge)
               (a/into [ ])))))
    (new-address! [this]
      (-> location
          wallet/load-wallet
          wallet/generate-address!)))

