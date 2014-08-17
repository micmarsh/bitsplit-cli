(ns bitsplit-cli.client
    (:use
        [bitsplit.client.protocol :only
        (Queries addresses unspent-amounts unspent-channel
         Operations send-amounts! new-address!)]
        [bitsplit.storage.protocol :only (all)]
        [bitsplit-cli.constants :only (DIR)]
        [bitsplit-cli.utils :only (call-method callback->channel)])
    (:require [cljs.core.async :refer (<!) :as a])
    (:use-macros
        [cljs.core.async.macros :only (go)]))

(def request
  (partial
    callback->channel
    (js/require "request")))

(def urls
  ["https://mainnet.helloblock.io/v1/addresses/$address$"
   "https://blockchain.info/address/$address$?format=json"])

(defn- balance [result]
  (or (.-final_balance result) ; blockchain
      (some-> result ; helloblock
        (.-data)
        (.-address)
        (.-confirmedBalance))))

(defn- parse-body [response]
  (->> response
    (second)
    (.parse js/JSON)))

(defn get-balance [urls address]
  (go
    (if-let [url (-> urls first (.replace "$address$" address))]
      (let [response (<! (request url))]
        (if-let [error (:error response)]
          (do
            (println "zomg error" error)
            (<! (get-balance (rest urls) address)))
          (-> response parse-body balance))))))

(defn address->balance [urls address]
  (go
    (let [amount (<! (get-balance urls address))]
      [address amount])))

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
        (if (empty? my-addrs)
          (go nil)
          (->> my-addrs
            (map (partial address->balance urls))
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
    (send-amounts! [this amounts]
        #_(a/merge
          (for [[from splits] amounts
                [to amount] splits
                :when (send? (.-fee coin) amount)]
            (let [account (aget (.-aaccounts coin) from)]
              (println from to amount)
              (call-method coin "sendFrom" account to amount)))))
    (new-address! [this]
        #_(-> coin
            .createAccount
            .getAddress)))

(defn- change-dust! [coin]
    (set! (.-dust coin) 1)
    coin)

(defn setup-save-loop! [coin]
  (js/setInterval
    #(.saveWallet coin) 10000)
  coin)

(defn new-client [location]
    (->Client #js {:addresses
        #js ["1LXv8VR7XMaCNAqui9hUicfsZqs4bGFpX4"
             "19eprtsSudARY78i9WRegjmG5DW5XTMZ4S"]})
    #_(-> {:db
          {:type "tiny" :path
            (str location "tinydb")}
         :wallet
            (str location "wallet.json")}
         clj->js
         coined
         change-dust!
         setup-save-loop!
         (->Client)))
