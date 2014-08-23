(ns bitsplit-cli.client
    (:require [bitsplit.client.protocol :refer
                (Queries addresses unspent-amounts unspent-channel
                 Operations send-amounts! new-address!)]
              [bitsplit.utils.calculate :refer (apply-percentages)]
              [bitsplit-cli.client.network :refer (address->unspents urls)]
              [bitsplit-cli.client.transactions :as tx]
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
            txs (tx/make-txs addrs)
            keys (private-keys wallet addrs)]
        (tx/with-inputs! txs unspents)
        (tx/with-outputs! txs totals)
        (tx/with-signatures! txs keys)
        (println "yay some new txs" txs)
        ; TODO here
        ;  * push txs to a server
        ))
    (new-address! [this] ))


(defn new-client [location]
    (->Client #js {:addresses
        #js ["1LXv8VR7XMaCNAqui9hUicfsZqs4bGFpX4"
             "19eprtsSudARY78i9WRegjmG5DW5XTMZ4S"]
        :getPrivateKeyForAddress (constantly "lolololololprivatekey")}))
