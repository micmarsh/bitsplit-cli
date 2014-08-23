(ns bitsplit-cli.client
    (:require [bitsplit.client.protocol :refer
                (Queries addresses unspent-amounts unspent-channel
                 Operations send-amounts! new-address!)]
              [bitsplit.utils.calculate :refer (apply-percentages)]
              [bitsplit-cli.client.network :refer (address->unspents urls)]
              [cljs.core.async :as a])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(def bitcoin (js/require "bitcoinjs-lib"))

(def Transaction (.-Transaction bitcoin))
(def ECKey (.-ECKey bitcoin))

(defn- new-txs []
  (cons (Transaction.) (lazy-seq (new-txs))))

(defn- make-txs [addresses]
  (zipmap addresses (new-txs)))

(defn add-inputs! [tx unspents]
  (let [tx (Transaction.)]
    (doseq [{:keys [tx-hash index]} unspents]
      (println tx-hash index unspents)
      (.addInput tx tx-hash index))
    tx))

(def with-inputs! (partial merge-with add-inputs!))

(defn- add-output! [tx send-to]
  (doseq [[address amount] send-to]
    (println "adding sending of" amount "to" address )
    (.addOutput tx address amount))
  tx)

(def with-outputs! (partial merge-with add-output!))

(defn sign! [tx private-key]
  (loop [inputs (.-in tx)
         i 0]
    (if (first inputs)
      (do
        (.sign tx i (ECKey. private-key))
        (recur (rest inputs) (inc i)))
      tx)))

(def with-signature! (partial merge-with sign!))


(defn- unspents->amounts [unspents]
  (into { }
    (map (fn [[address txs]]
      [address
        (->> txs (map :value) (reduce +))])
    unspents)))

(defn- send? [fee amount]
    (< (* 3 fee) amount))

(defn- safe-put! [channel item]
  (println "yay safe putting" item)
  (when-not (nil? item)
    (a/put! channel item)))


(defn- address->private  [wallet address]
  (.getPrivateKeyForAddress wallet address))

(defn- private-keys [wallet addresses]
  (let [privates (map (partial address->private wallet) addresses)]
    (zipmap addresses privates)))

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
            addrs (addresses this)
            amounts (unspents->amounts unspents)
            totals (apply-percentages percentages amounts) ; should prolly apply fee somewhere around here
            txs (new-txs addrs)
            keys (private-keys wallet addrs)]
        (with-outputs! txs unspents)
        (with-inputs! txs totals)
        (with-signature! txs keys)
        (println "yay some new txs" with-outputs priv-keys)
        ; TODO here
        ;  * then, will be able to add appropriate output address and amount for each tx
        ;  * notes on that: should group txs by address, so can sign w/ correct private keys
        ;    when the right time comes. according to examples, once for each input (thumbsup)
        ))
    (new-address! [this] ))


(defn new-client [location]
    (->Client #js {:addresses
        #js ["1LXv8VR7XMaCNAqui9hUicfsZqs4bGFpX4"
             "19eprtsSudARY78i9WRegjmG5DW5XTMZ4S"]
        :getPrivateKeyForAddress (constantly "lolololololprivatekey")}))
